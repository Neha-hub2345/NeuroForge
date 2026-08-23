package com.nexus.NeuroForge.services.monitoring;

// [M4][Jashanpreet] ExternalHealthMonitorService — the real replacement for
// simulated uptime. Polls every project's deployed app (Render, currently)
// on a fixed schedule and writes one HealthCheckResult per poll. Follows
// the same @Scheduled-service-loops-over-projects pattern as
// AlertMonitoringService / KpiSnapshotScheduler.
//
// A project with no monitor URL configured is silently skipped — not every
// project has a live target yet, and that's fine.

import com.nexus.NeuroForge.models.interfaces.DeploymentEnvironment;
import com.nexus.NeuroForge.models.monitoring.HealthCheckResult;
import com.nexus.NeuroForge.models.project.Project;
import com.nexus.NeuroForge.models.project.ProjectIntegration;
import com.nexus.NeuroForge.repositories.monitoring.HealthCheckResultRepository;
import com.nexus.NeuroForge.repositories.project.ProjectIntegrationRepository;
import com.nexus.NeuroForge.repositories.project.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class ExternalHealthMonitorService {

    private static final Logger log = LoggerFactory.getLogger(ExternalHealthMonitorService.class);

    @Autowired private ProjectRepository projectRepository;
    @Autowired private ProjectIntegrationRepository projectIntegrationRepository;
    @Autowired private com.nexus.NeuroForge.services.project.ProjectIntegrationService projectIntegrationService;
    @Autowired private HealthCheckResultRepository healthCheckResultRepository;
    @Autowired private RestTemplate restTemplate;

    // Same 30s cadence as AlertMonitoringService, since alert evaluation
    // reads whatever this poller just wrote — no point polling faster than
    // alerts are evaluated, or slower than a person would notice an outage.
    @Scheduled(fixedRate = 30000)
    public void pollAll() {
        for (Project project : projectRepository.findAll()) {
            try {
                pollProject(project);
            } catch (Exception e) {
                // One project's polling failure must never stop the loop
                // for the rest — log and move on.
                log.warn("Health poll failed unexpectedly for project {}: {}", project.getId(), e.getMessage());
            }
        }
    }

    // Called by RenderWebhookService right after a deploy notification comes
    // in, so a fresh deploy gets checked immediately rather than waiting up
    // to 30s for the next scheduled cycle. Silently no-ops for an unknown
    // project id — the caller already validated it exists.
    public void pollProjectNow(Long projectId) {
        projectRepository.findById(projectId).ifPresent(project -> {
            try {
                pollProject(project);
            } catch (Exception e) {
                log.warn("On-demand health poll failed for project {}: {}", projectId, e.getMessage());
            }
        });
    }

    private void pollProject(Project project) {
        Optional<ProjectIntegration> integrationOpt = projectIntegrationRepository.findByProject_Id(project.getId());
        if (integrationOpt.isEmpty()) return;

        ProjectIntegration integration = integrationOpt.get();
        String monitorUrl = integration.getMonitorUrl();
        if (monitorUrl == null || monitorUrl.isBlank()) return;

        // CHANGED (M4 step 7): MDC-scoped per project, same pattern as
        // AlertMonitoringService.evaluateRulesForProject — pollAll() loops
        // over every project on one scheduled thread, so this must be
        // cleared per-iteration or a slow project's projectId would still
        // be attached to the next project's log line.
        MDC.put("projectId", String.valueOf(project.getId()));
        try {
            String url = monitorUrl.endsWith("/") ? monitorUrl + "health" : monitorUrl + "/health";

            HealthCheckResult result = new HealthCheckResult();
            result.setProjectId(project.getId());
            result.setEnvironment(DeploymentEnvironment.PRODUCTION);
            result.setCheckedAt(LocalDateTime.now());

            HttpHeaders headers = new HttpHeaders();
            String token = projectIntegrationService.decryptMonitorToken(integration);
            if (token != null && !token.isBlank()) {
                headers.set("Authorization", "Bearer " + token);
            }

            long start = System.currentTimeMillis();
            try {
                ResponseEntity<String> response = restTemplate.exchange(
                        url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

                result.setResponseTimeMs(System.currentTimeMillis() - start);
                result.setStatusCode(response.getStatusCode().value());
                result.setUp(response.getStatusCode().is2xxSuccessful());

            } catch (RestClientResponseException e) {
                // Server responded, but with a non-2xx — reachable, just unhealthy.
                result.setResponseTimeMs(System.currentTimeMillis() - start);
                result.setStatusCode(e.getStatusCode().value());
                result.setUp(false);
                result.setErrorMessage(truncate(e.getMessage()));
                log.warn("Health check unhealthy: status={} url={}", e.getStatusCode().value(), url);

            } catch (ResourceAccessException e) {
                // Timeout, connection refused, DNS failure — target unreachable.
                result.setResponseTimeMs(System.currentTimeMillis() - start);
                result.setUp(false);
                result.setErrorMessage(truncate(e.getMessage()));
                log.warn("Health check unreachable: url={} error={}", url, e.getMessage());

            } catch (Exception e) {
                result.setResponseTimeMs(System.currentTimeMillis() - start);
                result.setUp(false);
                result.setErrorMessage(truncate(e.getMessage()));
                log.warn("Health check failed unexpectedly: url={} error={}", url, e.getMessage());
            }

            healthCheckResultRepository.save(result);
        } finally {
            MDC.remove("projectId");
        }
    }

    private String truncate(String message) {
        if (message == null) return null;
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
