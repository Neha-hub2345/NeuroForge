package com.nexus.NeuroForge.services.project;

import com.nexus.NeuroForge.dto.project.ProjectIntegrationRequest;
import com.nexus.NeuroForge.dto.project.ProjectIntegrationResponse;
import com.nexus.NeuroForge.models.project.Project;
import com.nexus.NeuroForge.models.project.ProjectIntegration;
import com.nexus.NeuroForge.repositories.project.ProjectIntegrationRepository;
import com.nexus.NeuroForge.repositories.project.ProjectRepository;
import com.nexus.NeuroForge.services.security.TokenEncryptionService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;

@Service
public class ProjectIntegrationService {

    @Autowired private ProjectIntegrationRepository integrationRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private TokenEncryptionService tokenEncryptionService;

    public ProjectIntegrationResponse getByProject(Long projectId) {
        ProjectIntegration integration = integrationRepository.findByProject_Id(projectId)
                .orElseThrow(() -> new EntityNotFoundException("No GitHub integration connected for project " + projectId));
        return toResponse(integration);
    }

    public ProjectIntegrationResponse connect(Long projectId, ProjectIntegrationRequest req) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found: " + projectId));

        ProjectIntegration integration = integrationRepository.findByProject_Id(projectId)
                .orElseGet(() -> {
                    ProjectIntegration fresh = new ProjectIntegration();
                    fresh.setProject(project);
                    fresh.setWebhookSecret(generateWebhookSecret());
                    return fresh;
                });

        integration.setGithubOwner(req.getGithubOwner());
        integration.setGithubRepo(req.getGithubRepo());
        integration.setGithubBranch(req.getGithubBranch() != null ? req.getGithubBranch() : "main");
        integration.setWorkflowFile(req.getWorkflowFile() != null ? req.getWorkflowFile() : "ci-cd.yml");

        // Only overwrite the stored token if the user actually typed a new one —
        // leaving the field blank on an edit keeps the existing connected token.
        if (req.getGithubToken() != null && !req.getGithubToken().isBlank()) {
            integration.setGithubTokenEncrypted(tokenEncryptionService.encrypt(req.getGithubToken()));
        }

        // M4: monitor URL can be set/cleared directly (it's not a secret).
        // Blank string clears it — a project can be un-monitored without
        // disconnecting GitHub entirely.
        if (req.getMonitorUrl() != null) {
            integration.setMonitorUrl(req.getMonitorUrl().isBlank() ? null : req.getMonitorUrl());
        }
        if (req.getMonitorToken() != null && !req.getMonitorToken().isBlank()) {
            integration.setMonitorTokenEncrypted(tokenEncryptionService.encrypt(req.getMonitorToken()));
        }

        return toResponse(integrationRepository.save(integration));
    }

    public ProjectIntegrationResponse regenerateWebhookSecret(Long projectId) {
        ProjectIntegration integration = integrationRepository.findByProject_Id(projectId)
                .orElseThrow(() -> new EntityNotFoundException("No GitHub integration connected for project " + projectId));
        integration.setWebhookSecret(generateWebhookSecret());
        return toResponse(integrationRepository.save(integration));
    }

    public void disconnect(Long projectId) {
        ProjectIntegration integration = integrationRepository.findByProject_Id(projectId)
                .orElseThrow(() -> new EntityNotFoundException("No GitHub integration connected for project " + projectId));
        integrationRepository.delete(integration);
    }

    // Used internally by PipelineService — never exposed over the API with
    // a decrypted token attached.
    public ProjectIntegration getEntityOrThrow(Long projectId) {
        return integrationRepository.findByProject_Id(projectId)
                .orElseThrow(() -> new IllegalStateException(
                        "Project " + projectId + " has no GitHub repository connected. Connect one from Project Settings."));
    }

    public String decryptToken(ProjectIntegration integration) {
        return tokenEncryptionService.decrypt(integration.getGithubTokenEncrypted());
    }

    // Used internally by ExternalHealthMonitorService. Returns null if no
    // monitor token was configured (some deployed apps may not require auth).
    public String decryptMonitorToken(ProjectIntegration integration) {
        return tokenEncryptionService.decrypt(integration.getMonitorTokenEncrypted());
    }

    private String generateWebhookSecret() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private ProjectIntegrationResponse toResponse(ProjectIntegration i) {
        return new ProjectIntegrationResponse(
                i.getId(),
                i.getProject().getId(),
                i.getGithubOwner(),
                i.getGithubRepo(),
                i.getGithubBranch(),
                i.getWorkflowFile(),
                i.getWebhookSecret(),
                i.getGithubTokenEncrypted() != null,
                i.getMonitorUrl(),
                i.getMonitorTokenEncrypted() != null
        );
    }
}