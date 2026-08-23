package com.nexus.NeuroForge.models.project;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "project_integrations")
public class ProjectIntegration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", unique = true, nullable = false)
    @JsonIgnore
    private Project project;

    private String githubOwner;
    private String githubRepo;
    private String githubBranch = "main";
    private String workflowFile = "ci-cd.yml";

    // Encrypted at rest — never serialized, never returned raw via API.
    @Column(name = "github_token_encrypted", columnDefinition = "TEXT")
    @JsonIgnore
    private String githubTokenEncrypted;

    // Given to the user to paste into their repo's Actions secrets.
    // Used to verify inbound webhook payloads (see WebhookSignatureValidator).
    @Column(name = "webhook_secret")
    private String webhookSecret;

    // --- M4: real-time health monitoring target ---
    // Base URL of the project's deployed app (e.g. a Render service) that
    // ExternalHealthMonitorService polls on a schedule. "{monitorUrl}/health"
    // is the endpoint hit. Optional — projects without a live monitoring
    // target simply get skipped by the poller.
    @Column(name = "monitor_url")
    private String monitorUrl;

    // Bearer token sent as "Authorization: Bearer <token>" to the monitored
    // app's /health endpoint, matching the auth scheme prom-client/the app
    // side already expects. Encrypted at rest via the same
    // TokenEncryptionService used for the GitHub token — never serialized.
    @Column(name = "monitor_token_encrypted", columnDefinition = "TEXT")
    @JsonIgnore
    private String monitorTokenEncrypted;




    public ProjectIntegration() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    public String getGithubOwner() { return githubOwner; }
    public void setGithubOwner(String githubOwner) { this.githubOwner = githubOwner; }
    public String getGithubRepo() { return githubRepo; }
    public void setGithubRepo(String githubRepo) { this.githubRepo = githubRepo; }
    public String getGithubBranch() { return githubBranch; }
    public void setGithubBranch(String githubBranch) { this.githubBranch = githubBranch; }
    public String getWorkflowFile() { return workflowFile; }
    public void setWorkflowFile(String workflowFile) { this.workflowFile = workflowFile; }
    public String getGithubTokenEncrypted() { return githubTokenEncrypted; }
    public void setGithubTokenEncrypted(String githubTokenEncrypted) { this.githubTokenEncrypted = githubTokenEncrypted; }
    public String getWebhookSecret() { return webhookSecret; }
    public void setWebhookSecret(String webhookSecret) { this.webhookSecret = webhookSecret; }
    public String getMonitorUrl() { return monitorUrl; }
    public void setMonitorUrl(String monitorUrl) { this.monitorUrl = monitorUrl; }
    public String getMonitorTokenEncrypted() { return monitorTokenEncrypted; }
    public void setMonitorTokenEncrypted(String monitorTokenEncrypted) { this.monitorTokenEncrypted = monitorTokenEncrypted; }
}