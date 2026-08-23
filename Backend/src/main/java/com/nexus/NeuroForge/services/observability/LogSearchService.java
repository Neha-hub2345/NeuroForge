// LogSearchService.java — [M4][Jashanpreet]
//
// Reads directly from Elasticsearch's own HTTP search API
// (http://elasticsearch:9200) over the internal Docker network — same
// pattern as PrometheusQueryService: the browser never talks to
// Elasticsearch or Kibana directly, the backend proxies it. Logstash
// writes one index per day (neuroforge-logs-YYYY.MM.dd, see
// logstash/logstash.conf), so we search the neuroforge-logs-* wildcard
// to cover a day rollover.
//
// xpack.security.enabled=false on the elasticsearch service in
// docker-compose.yml, so no credentials needed here — if that's ever
// turned on, this will need HTTP basic auth added to the request.
package com.nexus.NeuroForge.services.observability;

import com.nexus.NeuroForge.dto.observability.LogLineDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class LogSearchService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${app.elasticsearch.base-url:http://elasticsearch:9200}")
    private String elasticsearchBaseUrl;

    /**
     * Most recent log lines tagged with this project's id, newest first.
     * Returns an empty list on any failure (ES down, index not created
     * yet because nothing has logged today, malformed response) — this
     * backs a dashboard widget, not a critical path.
     */
    @SuppressWarnings("unchecked")
    public List<LogLineDTO> recentLogsForProject(String projectId, int limit) {
        try {
            URI uri = URI.create(elasticsearchBaseUrl + "/neuroforge-logs-*/_search");

            // projectId is indexed as analyzed text (see logstash.conf /
            // LogstashEncoder default mapping), so "match" — not "term" —
            // is the correct query type for an exact-looking value like
            // this against a text field.
            Map<String, Object> requestBody = Map.of(
                    "size", limit,
                    "sort", List.of(Map.of("@timestamp", "desc")),
                    "query", Map.of("match", Map.of("projectId", projectId))
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            Map<String, Object> body = restTemplate.exchange(uri, HttpMethod.POST, request, Map.class).getBody();
            if (body == null) return List.of();

            Map<String, Object> hitsWrapper = (Map<String, Object>) body.get("hits");
            if (hitsWrapper == null) return List.of();
            List<Map<String, Object>> hits = (List<Map<String, Object>>) hitsWrapper.get("hits");
            if (hits == null) return List.of();

            List<LogLineDTO> result = new ArrayList<>();
            for (Map<String, Object> hit : hits) {
                Map<String, Object> source = (Map<String, Object>) hit.get("_source");
                if (source == null) continue;
                result.add(new LogLineDTO(
                        String.valueOf(source.getOrDefault("@timestamp", "")),
                        String.valueOf(source.getOrDefault("level", "INFO")),
                        String.valueOf(source.getOrDefault("message", ""))
                ));
            }
            return result;
        } catch (Exception e) {
            return List.of();
        }
    }
}
