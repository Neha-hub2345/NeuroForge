package com.nexus.NeuroForge.controllers.deploy;

// [M4][Jashanpreet] Render deploy webhook — reuses the same per-project
// webhook secret and HMAC scheme as PipelineController's GitHub webhook
// (WebhookSignatureValidator), rather than introducing a second secret
// concept for a second webhook source.

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.NeuroForge.dto.deploy.RenderDeployWebhookRequest;
import com.nexus.NeuroForge.services.deploy.RenderWebhookService;
import com.nexus.NeuroForge.services.project.ProjectIntegrationService;
import com.nexus.NeuroForge.services.security.WebhookSignatureValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/render")
public class RenderWebhookController {

    private static final Logger log = LoggerFactory.getLogger(RenderWebhookController.class);

    @Autowired private RenderWebhookService renderWebhookService;
    @Autowired private ProjectIntegrationService projectIntegrationService;
    @Autowired private WebhookSignatureValidator webhookSignatureValidator;
    @Autowired private ObjectMapper objectMapper;

    // CHANGED (M4 step 7): same structured-logging treatment as
    // PipelineController's webhook — this is the only signal NeuroForge
    // gets that a Render deploy actually went live, so it's worth a
    // proper log trail in Kibana, not just a Notification row.
    @PostMapping("/deploy-webhook")
    public ResponseEntity<?> receiveDeployEvent(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature) {

        RenderDeployWebhookRequest request;
        try {
            request = objectMapper.readValue(rawBody, RenderDeployWebhookRequest.class);
        } catch (Exception e) {
            log.warn("Rejected render deploy webhook: malformed payload");
            return ResponseEntity.badRequest().body("Malformed webhook payload");
        }

        if (request.getProjectId() == null) {
            log.warn("Rejected render deploy webhook: missing projectId");
            return ResponseEntity.badRequest().body("projectId is required");
        }

        MDC.put("projectId", String.valueOf(request.getProjectId()));
        try {
            String webhookSecret;
            try {
                webhookSecret = projectIntegrationService.getEntityOrThrow(request.getProjectId()).getWebhookSecret();
            } catch (IllegalStateException e) {
                log.warn("Rejected render deploy webhook: unknown project");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
            }

            if (!webhookSignatureValidator.isValid(rawBody, signature, webhookSecret)) {
                log.warn("Rejected render deploy webhook: invalid signature");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid webhook signature");
            }

            renderWebhookService.handleDeployEvent(request);
            log.info("Render deploy webhook recorded: status={} commit={}",
                    request.getStatus(), request.getCommitHash());
            return ResponseEntity.ok(Map.of("status", "recorded"));
        } finally {
            MDC.remove("projectId");
        }
    }
}
