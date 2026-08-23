package com.nexus.NeuroForge.repositories.monitoring;

import com.nexus.NeuroForge.models.monitoring.HealthCheckResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface HealthCheckResultRepository extends JpaRepository<HealthCheckResult, Long> {

    // Rolling-window uptime calc: (up count / total count) since some cutoff.
    long countByProjectIdAndCheckedAtAfter(Long projectId, LocalDateTime after);
    long countByProjectIdAndUpTrueAndCheckedAtAfter(Long projectId, LocalDateTime after);

    // Powers a current-status indicator on the dashboard.
    Optional<HealthCheckResult> findTopByProjectIdOrderByCheckedAtDesc(Long projectId);

    // Powers a response-time / status history view, and Kibana correlation.
    List<HealthCheckResult> findByProjectIdAndCheckedAtAfterOrderByCheckedAtAsc(Long projectId, LocalDateTime after);

    // Average response time over the same rolling window, for a latency KPI.
    List<HealthCheckResult> findByProjectIdAndUpTrueAndCheckedAtAfter(Long projectId, LocalDateTime after);

    // Platform-wide equivalents — used by ReleaseService.getPlatformKpis(),
    // which feeds the global Micrometer gauges in ObservabilityConfig.
    long countByCheckedAtAfter(LocalDateTime after);
    long countByUpTrueAndCheckedAtAfter(LocalDateTime after);
}
