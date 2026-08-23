package com.nexus.NeuroForge.models.monitoring;

// [M4][Jashanpreet] HealthCheckResult — one row per poll of a project's
// deployed app (Render, in the current setup). Written by
// ExternalHealthMonitorService on a fixed schedule. This is the real,
// measured replacement for the rollback-derived uptime formula that used
// to live entirely inside ReleaseService.computeKpis().
// Mirrors the shape of KpiSnapshot for consistency with the rest of M4.

import com.nexus.NeuroForge.models.interfaces.DeploymentEnvironment;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "health_check_results")
public class HealthCheckResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long projectId;

    // Nullable for now — ProjectIntegration currently exposes one monitor
    // target per project rather than one per environment. Defaults to
    // PRODUCTION at write time. Kept on the entity so per-environment
    // monitoring can be added later without a migration.
    @Enumerated(EnumType.STRING)
    private DeploymentEnvironment environment;

    private LocalDateTime checkedAt;

    private boolean up;

    private long responseTimeMs;

    private Integer statusCode;

    // Populated on timeout / connection failure / non-2xx, so a dashboard
    // or Kibana can show *why* a check failed, not just that it did.
    @Column(length = 500)
    private String errorMessage;

    public HealthCheckResult() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public DeploymentEnvironment getEnvironment() { return environment; }
    public void setEnvironment(DeploymentEnvironment environment) { this.environment = environment; }
    public LocalDateTime getCheckedAt() { return checkedAt; }
    public void setCheckedAt(LocalDateTime checkedAt) { this.checkedAt = checkedAt; }
    public boolean isUp() { return up; }
    public void setUp(boolean up) { this.up = up; }
    public long getResponseTimeMs() { return responseTimeMs; }
    public void setResponseTimeMs(long responseTimeMs) { this.responseTimeMs = responseTimeMs; }
    public Integer getStatusCode() { return statusCode; }
    public void setStatusCode(Integer statusCode) { this.statusCode = statusCode; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
