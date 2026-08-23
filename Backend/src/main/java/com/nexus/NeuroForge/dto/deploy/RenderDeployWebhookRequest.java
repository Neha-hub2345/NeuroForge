// RenderDeployWebhookRequest.java — [M4][Jashanpreet] payload the Render-hosted
// Node.js app's deploy hook POSTs when Render finishes deploying a new version.
package com.nexus.NeuroForge.dto.deploy;

import java.time.LocalDateTime;

public class RenderDeployWebhookRequest {

    private Long projectId;

    // "live" | "deploy_failed" | "build_failed" — Render's own deploy
    // finished states, passed through as-is rather than re-mapped onto
    // PipelineStatus, since this isn't a pipeline run.
    private String status;

    private String commitHash;

    // Render's own deploy identifier, useful for cross-referencing Render's
    // dashboard/logs against what NeuroForge recorded.
    private String deployId;

    // Optional — if Render doesn't send it, the controller stamps "now".
    private LocalDateTime occurredAt;

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCommitHash() { return commitHash; }
    public void setCommitHash(String commitHash) { this.commitHash = commitHash; }
    public String getDeployId() { return deployId; }
    public void setDeployId(String deployId) { this.deployId = deployId; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }
}
