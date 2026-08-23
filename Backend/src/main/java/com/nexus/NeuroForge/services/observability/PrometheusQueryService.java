// PrometheusQueryService.java — [M4][Jashanpreet]
//
// Talks to Prometheus's own HTTP API (http://prometheus:9090/api/v1/query)
// over the internal Docker network — the browser never calls Prometheus
// directly, so there's no CORS surface to open up on that container just
// for one dashboard widget. Same RestTemplate bean PipelineService and
// ExternalHealthMonitorService already use for outbound calls.
//
// CHANGED: every query here always failed, silently, regardless of whether
// Prometheus was actually reachable — confirmed via Prometheus's own
// target-health page showing the scrape target UP the whole time. Cause:
// queryScalar built the request URL as a plain String and passed it to
// restTemplate.getForObject(String, Class), which makes RestTemplate treat
// the String as a URI *template* and try to expand any `{...}` in it as a
// template variable. Every PromQL label selector here is literally
// `metric{label="value"}` — those braces aren't URI templates, but
// RestTemplate can't tell the difference, so it threw
// IllegalArgumentException on every call ("not enough variable values"),
// which the try/catch below swallowed into Optional.empty(). Building a
// real java.net.URI up front and calling getForObject(URI, Class) instead
// skips template expansion entirely — the value is used as-is.
package com.nexus.NeuroForge.services.observability;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PrometheusQueryService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${app.prometheus.base-url:http://prometheus:9090}")
    private String prometheusBaseUrl;

    /**
     * Runs a PromQL instant query and returns the scalar value of the first
     * result series, if any. Swallows all failures (Prometheus down, no
     * such series yet, malformed response) and returns empty rather than
     * throwing — this backs a dashboard widget, not a critical path, and a
     * missing gauge value should render as "no data" in the UI, not a 500.
     */
    @SuppressWarnings("unchecked")
    public Optional<Double> queryScalar(String promql) {
        try {
            URI uri = UriComponentsBuilder
                    .fromHttpUrl(prometheusBaseUrl + "/api/v1/query")
                    .queryParam("query", promql)
                    .build()
                    .encode()
                    .toUri();

            Map<String, Object> body = restTemplate.getForObject(uri, Map.class);
            if (body == null || !"success".equals(body.get("status"))) return Optional.empty();

            Map<String, Object> data = (Map<String, Object>) body.get("data");
            List<Map<String, Object>> result = (List<Map<String, Object>>) data.get("result");
            if (result == null || result.isEmpty()) return Optional.empty();

            // Each result's "value" is [<unix timestamp>, "<string value>"]
            List<Object> value = (List<Object>) result.get(0).get("value");
            if (value == null || value.size() < 2) return Optional.empty();

            return Optional.of(Double.parseDouble(String.valueOf(value.get(1))));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /** Whether Prometheus currently considers our backend scrape target up. */
    public boolean isBackendTargetUp() {
        return queryScalar("up{job=\"neuroforge-backend\"}")
                .map(v -> v == 1.0)
                .orElse(false);
    }
}
