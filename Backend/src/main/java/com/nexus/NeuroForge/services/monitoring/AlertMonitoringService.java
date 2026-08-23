package com.nexus.NeuroForge.services.monitoring;

import com.nexus.NeuroForge.dto.alerts.AlertRuleRequest;
import com.nexus.NeuroForge.dto.pipeline.PipelineKpiDTO;
import com.nexus.NeuroForge.dto.release.ReleaseKpiDTO;
import com.nexus.NeuroForge.models.alert.Alert;
import com.nexus.NeuroForge.models.alert.AlertRule;
import com.nexus.NeuroForge.models.interfaces.AlertMetric;
import com.nexus.NeuroForge.models.interfaces.AlertStatus;
import com.nexus.NeuroForge.models.interfaces.Role;
import com.nexus.NeuroForge.models.notification.Notification;
import com.nexus.NeuroForge.models.project.Project;
import com.nexus.NeuroForge.models.user.User;
import com.nexus.NeuroForge.repositories.alert.AlertRepository;
import com.nexus.NeuroForge.repositories.alert.AlertRuleRepository;
import com.nexus.NeuroForge.repositories.notification.NotificationRepository;
import com.nexus.NeuroForge.repositories.project.ProjectRepository;
import com.nexus.NeuroForge.repositories.user.UserRepository;
import com.nexus.NeuroForge.services.pipeline.PipelineService;
import com.nexus.NeuroForge.services.releases.ReleaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class AlertMonitoringService {

    private static final Logger log = LoggerFactory.getLogger(AlertMonitoringService.class);

    @Autowired private AlertRuleRepository alertRuleRepository;
    @Autowired private AlertRepository alertRepository;
    @Autowired private ReleaseService releaseService;
    @Autowired private PipelineService pipelineService;
    @Autowired private UserRepository userRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private ProjectRepository projectRepository;

    @Scheduled(fixedRate = 30000)
    @Transactional
    public void evaluateRules() {
        for (Project project : projectRepository.findAll()) {
            evaluateRulesForProject(project.getId());
        }
    }

    // CHANGED (M4 step 7): wrapped in MDC.put/finally-remove per project —
    // this runs in a loop over every project on one scheduled thread
    // (evaluateRules), so without the finally block a projectId set for
    // project A would still be attached to log lines for project B on the
    // next loop iteration.
    private void evaluateRulesForProject(Long projectId) {
        MDC.put("projectId", String.valueOf(projectId));
        try {
            List<AlertRule> rules = alertRuleRepository.findByProjectIdAndEnabledTrue(projectId);
            if (rules.isEmpty()) return;

            Map<AlertMetric, Double> currentValues = currentMetricValues(projectId);

            for (AlertRule rule : rules) {
                Double value = currentValues.get(rule.getMetric());
                if (value == null) continue;

                boolean breached = isBreached(value, rule.getThresholdValue(), rule.getOperator());
                List<Alert> active = alertRepository.findByProjectIdAndMetricAndStatus(projectId, rule.getMetric(), AlertStatus.ACTIVE);

                if (breached && active.isEmpty()) {
                    Alert alert = new Alert();
                    alert.setProjectId(projectId);
                    alert.setMetric(rule.getMetric());
                    alert.setSeverity(rule.getSeverity());
                    alert.setStatus(AlertStatus.ACTIVE);
                    alert.setValue(value);
                    alert.setThreshold(rule.getThresholdValue());
                    alert.setTriggeredAt(LocalDateTime.now());
                    alert.setMessage(String.format("%s is %.2f (threshold %s %.2f)",
                            rule.getMetric(), value, rule.getOperator(), rule.getThresholdValue()));
                    alertRepository.save(alert);
                    log.warn("Alert triggered: metric={} value={} threshold={} {} severity={}",
                            rule.getMetric(), value, rule.getOperator(), rule.getThresholdValue(), rule.getSeverity());
                    notifyAdmins("ALERT_TRIGGERED", alert.getMessage());
                } else if (!breached && !active.isEmpty()) {
                    for (Alert alert : active) {
                        alert.setStatus(AlertStatus.RESOLVED);
                        alert.setResolvedAt(LocalDateTime.now());
                        alertRepository.save(alert);
                        log.info("Alert resolved: metric={} value={} alertId={}",
                                rule.getMetric(), value, alert.getId());
                        notifyAdmins("ALERT_RESOLVED", rule.getMetric() + " returned to normal (" + value + ")");
                    }
                }
            }
        } finally {
            MDC.remove("projectId");
        }
    }

    private Map<AlertMetric, Double> currentMetricValues(Long projectId) {
        ReleaseKpiDTO r = releaseService.getKpis(projectId);
        PipelineKpiDTO p = pipelineService.getKpis(projectId);
        Map<AlertMetric, Double> values = new EnumMap<>(AlertMetric.class);
        values.put(AlertMetric.UPTIME_PERCENT, r.uptimePercent);
        values.put(AlertMetric.MTTR_MINUTES, r.mttrMinutes);
        values.put(AlertMetric.RELEASES_THIS_MONTH, (double) r.releasesThisMonth);
        values.put(AlertMetric.ROLLED_BACK_RELEASES, (double) r.rolledBackReleases);
        values.put(AlertMetric.PIPELINE_SUCCESS_RATE, p.getSuccessRate());
        values.put(AlertMetric.AVG_DEPLOY_MINUTES, p.getAvgDeployTimeMinutes());
        return values;
    }

    public List<Alert> getAllAlerts(Long projectId) { return alertRepository.findByProjectIdOrderByTriggeredAtDesc(projectId); }
    public List<AlertRule> getAllRules(Long projectId) { return alertRuleRepository.findByProjectId(projectId); }

    public AlertRule createRule(Long projectId, AlertRuleRequest req) {
        AlertRule rule = new AlertRule();
        rule.setProjectId(projectId);
        applyRequest(rule, req);
        return alertRuleRepository.save(rule);
    }

    private boolean isBreached(double value, double threshold, com.nexus.NeuroForge.models.interfaces.AlertOperator op) {
        return switch (op) {
            case GT -> value > threshold;
            case LT -> value < threshold;
            case GTE -> value >= threshold;
            case LTE -> value <= threshold;
        };
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


    public AlertRule updateRule(Long id, AlertRuleRequest req) {
        AlertRule rule = alertRuleRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Rule not found: " + id));
        applyRequest(rule, req);
        return alertRuleRepository.save(rule);
    }

    public void deleteRule(Long id) { alertRuleRepository.deleteById(id); }

    private void applyRequest(AlertRule rule, AlertRuleRequest req) {
        rule.setMetric(req.getMetric());
        rule.setOperator(req.getOperator());
        rule.setThresholdValue(req.getThresholdValue());
        rule.setSeverity(req.getSeverity());
        rule.setEnabled(req.isEnabled());
    }
}