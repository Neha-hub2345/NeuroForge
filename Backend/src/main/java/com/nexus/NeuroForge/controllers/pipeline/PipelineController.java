package com.nexus.NeuroForge.controllers.pipeline;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.NeuroForge.dto.pipeline.LiveBuildStatusDTO;
import com.nexus.NeuroForge.dto.pipeline.PipelineDetailDTO;
import com.nexus.NeuroForge.dto.pipeline.PipelineKpiDTO;
import com.nexus.NeuroForge.dto.pipeline.PipelineResponse;
import com.nexus.NeuroForge.dto.pipeline.PipelineWebhookRequest;
import com.nexus.NeuroForge.models.pipeline.Pipeline;
import com.nexus.NeuroForge.services.pipeline.PipelineService;
import com.nexus.NeuroForge.services.project.ProjectIntegrationService;
import com.nexus.NeuroForge.services.security.WebhookSignatureValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pipelines")
public class PipelineController {

    private static final Logger log = LoggerFactory.getLogger(PipelineController.class);

    @Autowired private PipelineService pipelineService;
    @Autowired private ProjectIntegrationService projectIntegrationService;
    @Autowired private WebhookSignatureValidator webhookSignatureValidator;
    @Autowired private ObjectMapper objectMapper;

    // CHANGED (M4 step 7): added structured log lines around this
    // webhook — this is the entry point of the entire push -> CI ->
    // release flow, and previously logged nothing at all, so Kibana had
    // no record of pipeline events actually happening. MDC "projectId" is
    // scoped to this request only (cleared in finally), so it never leaks
    // onto an unrelated request on a pooled thread.
    @PostMapping("/webhook")
    public ResponseEntity<?> receiveBuildResult(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature) {

        PipelineWebhookRequest request;
        try {
            request = objectMapper.readValue(rawBody, PipelineWebhookRequest.class);
        } catch (Exception e) {
            log.warn("Rejected pipeline webhook: malformed payload");
            return ResponseEntity.badRequest().body("Malformed webhook payload");
        }

        if (request.getProjectId() == null) {
            log.warn("Rejected pipeline webhook: missing projectId");
            return ResponseEntity.badRequest().body("projectId is required");
        }

        MDC.put("projectId", String.valueOf(request.getProjectId()));
        try {
            String webhookSecret;
            try {
                webhookSecret = projectIntegrationService.getEntityOrThrow(request.getProjectId()).getWebhookSecret();
            } catch (IllegalStateException e) {
                log.warn("Rejected pipeline webhook: unknown project");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
            }

            if (!webhookSignatureValidator.isValid(rawBody, signature, webhookSecret)) {
                log.warn("Rejected pipeline webhook: invalid signature");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid webhook signature");
            }

            Pipeline saved = pipelineService.recordBuildResult(request);
            log.info("Pipeline webhook recorded: pipelineId={} branch={} commit={}",
                    saved.getId(), saved.getBranch(), saved.getCommitHash());
            return ResponseEntity.ok(Map.of(
                    "status", "recorded",
                    "pipelineId", saved.getId()
            ));
        } finally {
            MDC.remove("projectId");
        }
    }
    @GetMapping
    public List<PipelineResponse> getHistory(@RequestParam Long projectId) {
        return pipelineService.getHistory(projectId);
    }

    @GetMapping("/kpi")
    public PipelineKpiDTO getKpis(@RequestParam Long projectId) {
        return pipelineService.getKpis(projectId);
    }

    // Polled by the frontend while a build is running to tail its real
    // GitHub Actions logs. Once the run completes, the finish webhook
    // above (recordBuildResult) creates the normal Pipeline row and the
    // frontend switches back to the regular history/detail view.
    @GetMapping("/live")
    public LiveBuildStatusDTO getLiveStatus(@RequestParam Long projectId) {
        return pipelineService.getLiveStatus(projectId);
    }

    @GetMapping("/{id}")
    public PipelineDetailDTO getDetail(@PathVariable Long id) {
        return pipelineService.getDetail(id);
    }

    @PostMapping("/trigger/{projectId}")
    public ResponseEntity<String> triggerPipeline(@PathVariable Long projectId) {
        pipelineService.triggerJenkinsBuild(projectId);
        return ResponseEntity.ok("Pipeline triggered successfully");
    }

    @PostMapping("/{pipelineId}/rollback")
    public ResponseEntity<String> rollbackDeployment(@PathVariable Long pipelineId) {
        pipelineService.executeRollback(pipelineId);
        return ResponseEntity.ok("Rollback initiated");
    }
}