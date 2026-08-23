package com.nexus.NeuroForge.services.deploy;

// [M4][Jashanpreet] RenderWebhookService — business logic behind
// POST /api/render/deploy-webhook. Render's own deploy cycle is invisible
// to GitHub Actions (Render deploys itself off the connected repo), so this
// is the only signal NeuroForge gets that a new version actually went live.
// Two things happen on a deploy notification:
//   1. An immediate health poll, so uptime reflects the new version within
//      seconds instead of waiting up to 30s for the next scheduled cycle.
//   2. A notification to admins/PMs, same audience AlertMonitoringService
//      already notifies, so a failed Render deploy surfaces the same way
//      an alert would.

import com.nexus.NeuroForge.dto.deploy.RenderDeployWebhookRequest;
import com.nexus.NeuroForge.models.interfaces.Role;
import com.nexus.NeuroForge.models.notification.Notification;
import com.nexus.NeuroForge.models.user.User;
import com.nexus.NeuroForge.repositories.notification.NotificationRepository;
import com.nexus.NeuroForge.repositories.user.UserRepository;
import com.nexus.NeuroForge.services.monitoring.ExternalHealthMonitorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RenderWebhookService {

    @Autowired private ExternalHealthMonitorService externalHealthMonitorService;
    @Autowired private UserRepository userRepository;
    @Autowired private NotificationRepository notificationRepository;

    public void handleDeployEvent(RenderDeployWebhookRequest req) {
        LocalDateTime occurredAt = req.getOccurredAt() != null ? req.getOccurredAt() : LocalDateTime.now();
        boolean succeeded = "live".equalsIgnoreCase(req.getStatus());

        String message = succeeded
                ? String.format("Render deploy went live for project %d (commit %s) at %s",
                        req.getProjectId(), shortHash(req.getCommitHash()), occurredAt)
                : String.format("Render deploy failed for project %d: %s (commit %s)",
                        req.getProjectId(), req.getStatus(), shortHash(req.getCommitHash()));

        notifyAdmins(succeeded ? "RENDER_DEPLOY_LIVE" : "RENDER_DEPLOY_FAILED", message);

        // Only worth an immediate check if the deploy actually succeeded —
        // a failed Render build doesn't change what's currently serving
        // traffic, so the next scheduled poll is soon enough.
        if (succeeded) {
            externalHealthMonitorService.pollProjectNow(req.getProjectId());
        }
    }

    private String shortHash(String commitHash) {
        if (commitHash == null || commitHash.isBlank()) return "unknown";
        return commitHash.length() > 7 ? commitHash.substring(0, 7) : commitHash;
    }

    private void notifyAdmins(String type, String message) {
        List<User> targets = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ADMIN || u.getRole() == Role.PROJECT_MANAGER)
                .toList();
        for (User u : targets) {
            Notification n = new Notification();
            n.setType(type);
            n.setMessage(message);
            n.setUserId(u);
            notificationRepository.save(n);
        }
    }
}
