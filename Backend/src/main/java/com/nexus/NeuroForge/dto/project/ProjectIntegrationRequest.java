package com.nexus.NeuroForge.dto.project;

public class ProjectIntegrationRequest {
    private String githubOwner;
    private String githubRepo;
    private String githubBranch;
    private String workflowFile;
    // Plaintext, only ever sent client -> server, never returned back.
    private String githubToken;

    // M4: optional live health-monitoring target for the deployed app.
    private String monitorUrl;
    // Plaintext, only ever sent client -> server, never returned back.
    private String monitorToken;

    public String getGithubOwner() { return githubOwner; }
    public void setGithubOwner(String githubOwner) { this.githubOwner = githubOwner; }
    public String getGithubRepo() { return githubRepo; }
    public void setGithubRepo(String githubRepo) { this.githubRepo = githubRepo; }
    public String getGithubBranch() { return githubBranch; }
    public void setGithubBranch(String githubBranch) { this.githubBranch = githubBranch; }
    public String getWorkflowFile() { return workflowFile; }
    public void setWorkflowFile(String workflowFile) { this.workflowFile = workflowFile; }
    public String getGithubToken() { return githubToken; }
    public void setGithubToken(String githubToken) { this.githubToken = githubToken; }
    public String getMonitorUrl() { return monitorUrl; }
    public void setMonitorUrl(String monitorUrl) { this.monitorUrl = monitorUrl; }
    public String getMonitorToken() { return monitorToken; }
    public void setMonitorToken(String monitorToken) { this.monitorToken = monitorToken; }
}