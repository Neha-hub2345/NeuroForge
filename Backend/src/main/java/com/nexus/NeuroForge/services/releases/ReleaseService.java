package com.nexus.NeuroForge.services.releases;

import com.nexus.NeuroForge.dto.release.CreateReleaseRequest;
import com.nexus.NeuroForge.dto.release.ReleaseDetailDTO;
import com.nexus.NeuroForge.dto.release.ReleaseKpiDTO;
import com.nexus.NeuroForge.dto.release.ReleaseResponse;
import com.nexus.NeuroForge.models.deploy.Deployment;
import com.nexus.NeuroForge.models.releases.Release;
import com.nexus.NeuroForge.models.interfaces.DeploymentEnvironment;
import com.nexus.NeuroForge.models.interfaces.DeploymentSlot;
import com.nexus.NeuroForge.models.interfaces.ReleaseStatus;
import com.nexus.NeuroForge.repositories.deploy.DeploymentRepository;
import com.nexus.NeuroForge.repositories.releases.ReleaseRepository;
import com.nexus.NeuroForge.services.pipeline.PipelineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ReleaseService {

    private static final Logger log = LoggerFactory.getLogger(ReleaseService.class);

    @Autowired private ReleaseRepository releaseRepository;
    @Autowired private DeploymentRepository deploymentRepository;
    @Autowired private com.nexus.NeuroForge.repositories.monitoring.HealthCheckResultRepository healthCheckResultRepository;

    // Rolling window for the real, measured uptime calc. 24h balances
    // responsiveness (a bad release shows up in the number within a day)
    // against noise from a single blip.
    private static final long UPTIME_WINDOW_HOURS = 24L;

    // PipelineService already owns the GitHub Actions dispatch + rollback-eligibility
    // logic from M3 (executeRollback). ReleaseService reuses it rather than
    // re-implementing the workflow trigger, and layers the release-record
    // bookkeeping (blue-green slot swap, status transitions) on top.
    @Autowired private PipelineService pipelineService;

    /**
     * Cuts a new Release from a successful Deployment and promotes it to
     * live traffic in its environment (the "green" side goes live, the
     * previously active release becomes the standby "blue" side, or vice
     * versa). Scoped per-project: the "currently active" lookup for the
     * blue-green swap only considers releases belonging to the SAME
     * project as the deployment being released, so two projects deploying
     * to the same environment name (e.g. both to STAGING) never step on
     * each other's active release.
     */
    public Release createRelease(CreateReleaseRequest req) {
        Deployment deployment = deploymentRepository.findById(req.getDeploymentId())
                .orElseThrow(() -> new IllegalArgumentException("No deployment found with id " + req.getDeploymentId()));

        if (!deployment.isSuccess()) {
            throw new IllegalStateException("Cannot cut a release from a deployment that did not succeed.");
        }

        if (releaseRepository.findByDeployment_Id(deployment.getId()).isPresent()) {
            throw new IllegalStateException("A release already exists for this deployment.");
        }

        Long projectId = deployment.getPipeline().getProject().getId();
        DeploymentEnvironment env = deployment.getEnvironment();

        // MDC scoped to this call, not the whole request — createRelease
        // can also be invoked from non-HTTP contexts later, so it shouldn't
        // assume RequestCorrelationFilter already set/will clear this key.
        MDC.put("projectId", String.valueOf(projectId));
        try {
            Optional<Release> currentlyActive = releaseRepository
                    .findTopByEnvironmentAndActiveTrueAndDeployment_Pipeline_Project_IdOrderByReleaseDateDesc(env, projectId);

            Release release = new Release();
            release.setDeployment(deployment);
            release.setVersion(deployment.getImageTag() != null ? deployment.getImageTag() : "deploy-" + deployment.getId());
            release.setApproved(req.isApproved());
            release.setReleaseDate(LocalDateTime.now());
            release.setEnvironment(env);
            release.setStatus(ReleaseStatus.DEPLOYED);
            release.setActive(true);
            release.setSlot(currentlyActive
                    .map(r -> r.getSlot() == DeploymentSlot.BLUE ? DeploymentSlot.GREEN : DeploymentSlot.BLUE)
                    .orElse(DeploymentSlot.BLUE));

            releaseRepository.save(release);

            // Blue-green swap: the old active release stands down but stays in
            // history (not rolled back) so it can be reactivated instantly if
            // this new one needs to be rolled back later.
            currentlyActive.ifPresent(prev -> {
                prev.setActive(false);
                prev.setStatus(ReleaseStatus.SUPERSEDED);
                releaseRepository.save(prev);
            });

            log.info("Release cut: releaseId={} version={} env={} slot={} supersedes={}",
                    release.getId(), release.getVersion(), env, release.getSlot(),
                    currentlyActive.map(Release::getId).orElse(null));

            return release;
        } finally {
            MDC.remove("projectId");
        }
    }

    /**
     * Rolls back the currently active release in its environment: triggers
     * the actual GitHub Actions rollback workflow via PipelineService (same
     * mechanism M3 already validates), then flips the blue-green slots back
     * so the previous release becomes active again. The "previous release"
     * lookup is scoped to the same project as the release being rolled
     * back, for the same isolation reason as createRelease above.
     */
    public void rollbackRelease(Long releaseId) {
        Release release = releaseRepository.findById(releaseId)
                .orElseThrow(() -> new IllegalArgumentException("No release found with id " + releaseId));

        if (!release.isActive()) {
            throw new IllegalStateException("Only the currently active release can be rolled back.");
        }

        Long pipelineId = release.getDeployment().getPipeline().getId();
        Long projectId = release.getDeployment().getPipeline().getProject().getId();

        MDC.put("projectId", String.valueOf(projectId));
        try {
            log.warn("Rollback initiated: releaseId={} version={} env={}",
                    release.getId(), release.getVersion(), release.getEnvironment());

            pipelineService.executeRollback(pipelineId); // dispatches the real rollback workflow

            release.setActive(false);
            release.setStatus(ReleaseStatus.ROLLED_BACK);
            releaseRepository.save(release);

            // Reactivate the most recently superseded release in the same
            // environment AND same project — that's the image the rollback
            // workflow just redeployed.
            Optional<Release> reactivated = releaseRepository
                    .findByEnvironmentAndDeployment_Pipeline_Project_IdOrderByReleaseDateDesc(
                            release.getEnvironment(), projectId).stream()
                    .filter(r -> r.getStatus() == ReleaseStatus.SUPERSEDED)
                    .findFirst();

            reactivated.ifPresent(prev -> {
                prev.setActive(true);
                prev.setStatus(ReleaseStatus.DEPLOYED);
                releaseRepository.save(prev);
            });

            log.warn("Rollback complete: rolledBackReleaseId={} reactivatedReleaseId={}",
                    release.getId(), reactivated.map(Release::getId).orElse(null));
        } finally {
            MDC.remove("projectId");
        }
    }

    public Release getActiveRelease(Long projectId, DeploymentEnvironment environment) {
        return releaseRepository.findTopByEnvironmentAndActiveTrueAndDeployment_Pipeline_Project_IdOrderByReleaseDateDesc(
                        environment, projectId)
                .orElseThrow(() -> new IllegalStateException("No active release for environment " + environment));
    }

    public List<ReleaseResponse> getHistory(Long projectId) {
        return releaseRepository.findByDeployment_Pipeline_Project_IdOrderByReleaseDateDesc(projectId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ReleaseDetailDTO getDetail(Long id) {
        Release r = releaseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No release found with id " + id));

        ReleaseDetailDTO dto = new ReleaseDetailDTO();
        dto.id = r.getId();
        dto.version = r.getVersion();
        dto.environment = r.getEnvironment() != null ? r.getEnvironment().name() : null;
        dto.status = r.getStatus() != null ? r.getStatus().name() : null;
        dto.slot = r.getSlot() != null ? r.getSlot().name() : null;
        dto.active = r.isActive();
        dto.approved = r.isApproved();
        dto.releaseDate = r.getReleaseDate();

        Deployment d = r.getDeployment();
        if (d != null) {
            var di = new ReleaseDetailDTO.DeploymentInfo();
            di.id = d.getId();
            di.imageTag = d.getImageTag();
            di.podsRunning = d.getPodsRunning();
            di.podsTotal = d.getPodsTotal();
            di.cpuPercent = d.getCpuPercent();
            di.memoryPercent = d.getMemoryPercent();
            di.success = d.isSuccess();
            dto.deployment = di;

            if (d.getPipeline() != null) {
                var pi = new ReleaseDetailDTO.PipelineInfo();
                pi.id = d.getPipeline().getId();
                pi.branch = d.getPipeline().getBranch();
                pi.commitHash = d.getPipeline().getCommitHash();
                pi.commitMessage = d.getPipeline().getCommitMessage();
                dto.pipeline = pi;
            }
        }

        return dto;
    }

    /**
     * KPI computation (per Milestone 4 spec): uptime% now comes from real
     * ExternalHealthMonitorService poll results when a project has a
     * monitor target configured — (up checks / total checks) over a
     * rolling {@link #UPTIME_WINDOW_HOURS} window. Falls back to the
     * rollback-derived estimate for projects with no HealthCheckResult
     * rows yet (no monitor URL configured), so the KPI never silently
     * drops to a misleading 0%. MTTR still comes from release/rollback
     * history. These numbers are exposed as real gauges (see
     * ObservabilityConfig) so Prometheus can scrape and Grafana can chart
     * them like any other metric.
     *
     * Scoped per-project: cache keys are project IDs, since KPIs are now
     * computed per-project rather than globally across the whole platform.
     */
    // Prometheus scrapes /actuator/prometheus roughly every 10-15s, and
    // ObservabilityConfig registers gauges that each call getKpis() —
    // without this, one scrape = a full table scan + aggregation per
    // project every time. Cached for a few seconds per-project so a
    // scrape (or a burst of dashboard requests) only recomputes once per
    // project; still short enough that a brand-new release or rollback
    // shows up almost immediately, so it's not "hardcoded", just debounced.
    private final Map<Long, ReleaseKpiDTO> cachedKpis = new ConcurrentHashMap<>();
    private final Map<Long, Long> cachedKpisAt = new ConcurrentHashMap<>();
    private static final long KPI_CACHE_MS = 5000L;

    public ReleaseKpiDTO getKpis(Long projectId) {
        long now = System.currentTimeMillis();
        Long lastAt = cachedKpisAt.get(projectId);
        if (lastAt != null && (now - lastAt) < KPI_CACHE_MS) {
            ReleaseKpiDTO cached = cachedKpis.get(projectId);
            if (cached != null) {
                return cached;
            }
        }
        ReleaseKpiDTO fresh = computeKpis(projectId);
        cachedKpis.put(projectId, fresh);
        cachedKpisAt.put(projectId, now);
        return fresh;
    }

    private ReleaseKpiDTO computeKpis(Long projectId) {
        List<Release> all = releaseRepository.findByDeployment_Pipeline_Project_IdOrderByReleaseDateDesc(projectId);
        long total = all.size();

        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        long releasesThisMonth = releaseRepository.countByReleaseDateBetweenAndDeployment_Pipeline_Project_Id(
                monthStart, LocalDateTime.now(), projectId);

        List<Release> rolledBack = all.stream()
                .filter(r -> r.getStatus() == ReleaseStatus.ROLLED_BACK)
                .collect(Collectors.toList());
        long rolledBackCount = rolledBack.size();

        double mttrMinutes = rolledBack.stream()
                .mapToDouble(r -> minutesToRecovery(r, projectId))
                .filter(m -> m >= 0)
                .average()
                .orElse(0);

        double incidentRate = total == 0 ? 0 : (double) rolledBackCount / total;
        double uptimePercent = computeUptimePercent(projectId, incidentRate);

        return new ReleaseKpiDTO(releasesThisMonth, round(uptimePercent), round(mttrMinutes), total, rolledBackCount);
    }

    // Real uptime when health-check data exists for this project, else the
    // rollback-derived estimate. This is the swap-in point described in the
    // class-level comment above — callers (AlertMonitoringService,
    // ObservabilityConfig's gauges) don't need to change at all, since they
    // only ever read the resulting ReleaseKpiDTO.uptimePercent.
    private double computeUptimePercent(Long projectId, double incidentRateFallback) {
        LocalDateTime windowStart = LocalDateTime.now().minusHours(UPTIME_WINDOW_HOURS);
        long totalChecks = healthCheckResultRepository.countByProjectIdAndCheckedAtAfter(projectId, windowStart);

        if (totalChecks == 0) {
            // No monitor URL configured for this project yet, or the
            // poller hasn't run since it was added — fall back rather than
            // report a misleading 0%.
            return Math.max(0, 100.0 - (incidentRateFallback * 5.0));
        }

        long upChecks = healthCheckResultRepository.countByProjectIdAndUpTrueAndCheckedAtAfter(projectId, windowStart);
        return (double) upChecks / totalChecks * 100.0;
    }

    // Time between a rolled-back release going live and the replacement
    // release (the one that superseded it, redeployed after rollback)
    // going live — i.e. how long the bad release was serving traffic.
    // Scoped to the same project as the rolled-back release.
    private double minutesToRecovery(Release rolledBackRelease, Long projectId) {
        return releaseRepository.findByEnvironmentAndDeployment_Pipeline_Project_IdOrderByReleaseDateDesc(
                        rolledBackRelease.getEnvironment(), projectId).stream()
                .filter(r -> r.getReleaseDate().isAfter(rolledBackRelease.getReleaseDate()))
                .min(Comparator.comparing(Release::getReleaseDate))
                .map(next -> (double) Duration.between(rolledBackRelease.getReleaseDate(), next.getReleaseDate()).toMinutes())
                .orElse(-1.0);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public ReleaseResponse toResponse(Release r) {
        Deployment d = r.getDeployment();
        return new ReleaseResponse(
                r.getId(), r.getVersion(),
                r.getEnvironment() != null ? r.getEnvironment().name() : null,
                r.getStatus() != null ? r.getStatus().name() : null,
                r.getSlot() != null ? r.getSlot().name() : null,
                r.isActive(), r.isApproved(), r.getReleaseDate(),
                d != null ? d.getId() : null,
                d != null && d.getPipeline() != null ? d.getPipeline().getId() : null
        );
    }




    /**
     * Platform-wide KPIs, aggregated across ALL projects — used by
     * ObservabilityConfig's Prometheus gauges, which are single global values
     * and can't be parameterized per scrape. Per-project KPIs (used by the
     * dashboard UI) go through getKpis(Long projectId) instead.
     */
    public ReleaseKpiDTO getPlatformKpis() {
        long now = System.currentTimeMillis();
        if (cachedPlatformKpis != null && (now - cachedPlatformKpisAt) < KPI_CACHE_MS) {
            return cachedPlatformKpis;
        }
        ReleaseKpiDTO fresh = computePlatformKpis();
        cachedPlatformKpis = fresh;
        cachedPlatformKpisAt = now;
        return fresh;
    }

    private volatile ReleaseKpiDTO cachedPlatformKpis;
    private volatile long cachedPlatformKpisAt = 0L;

    private ReleaseKpiDTO computePlatformKpis() {
        List<Release> all = releaseRepository.findAllByOrderByReleaseDateDesc();
        long total = all.size();

        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        long releasesThisMonth = releaseRepository.countByReleaseDateBetween(monthStart, LocalDateTime.now());

        List<Release> rolledBack = all.stream()
                .filter(r -> r.getStatus() == ReleaseStatus.ROLLED_BACK)
                .collect(Collectors.toList());
        long rolledBackCount = rolledBack.size();

        double mttrMinutes = rolledBack.stream()
                .mapToDouble(this::minutesToRecoveryPlatformWide)
                .filter(m -> m >= 0)
                .average()
                .orElse(0);

        double incidentRate = total == 0 ? 0 : (double) rolledBackCount / total;
        double uptimePercent = computePlatformUptimePercent(incidentRate);

        return new ReleaseKpiDTO(releasesThisMonth, round(uptimePercent), round(mttrMinutes), total, rolledBackCount);
    }

    // Platform-wide equivalent of computeUptimePercent — feeds
    // ObservabilityConfig's global gauges, which can't be scoped to a
    // single project.
    private double computePlatformUptimePercent(double incidentRateFallback) {
        LocalDateTime windowStart = LocalDateTime.now().minusHours(UPTIME_WINDOW_HOURS);
        long totalChecks = healthCheckResultRepository.countByCheckedAtAfter(windowStart);

        if (totalChecks == 0) {
            return Math.max(0, 100.0 - (incidentRateFallback * 5.0));
        }

        long upChecks = healthCheckResultRepository.countByUpTrueAndCheckedAtAfter(windowStart);
        return (double) upChecks / totalChecks * 100.0;
    }

    private double minutesToRecoveryPlatformWide(Release rolledBackRelease) {
        return releaseRepository.findByEnvironmentOrderByReleaseDateDesc(rolledBackRelease.getEnvironment()).stream()
                .filter(r -> r.getReleaseDate().isAfter(rolledBackRelease.getReleaseDate()))
                .min(Comparator.comparing(Release::getReleaseDate))
                .map(next -> (double) Duration.between(rolledBackRelease.getReleaseDate(), next.getReleaseDate()).toMinutes())
                .orElse(-1.0);
    }






}