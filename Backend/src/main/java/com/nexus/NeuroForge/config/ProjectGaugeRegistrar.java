package com.nexus.NeuroForge.config;

// [M4][Jashanpreet] Step 5 — per-project Micrometer gauges.
//
// ObservabilityConfig registers the 4 release KPI gauges exactly once, at
// startup, aggregated across all projects. That's fine for a single global
// panel, but Grafana can't filter/template by project unless each series
// carries a project_id tag. Gauges can't just be registered once per
// project at startup the way ObservabilityConfig does it, because the
// project list is dynamic — projects get created (and cascade-deleted,
// see models/project/Project.java) throughout the app's lifetime, not just
// at boot.
//
// So this runs on a schedule instead: each tick, diff the live project list
// against what's currently registered, add gauges for new projects, and
// remove gauges for projects that no longer exist. Piggybacks on the same
// dedicated scheduler pool as ExternalHealthMonitorService (see
// SchedulingConfig) so a slow tick here can't delay alert evaluation.
//
// Gauge.builder's state-function form (registered once, re-read on every
// Prometheus scrape) is intentional: ReleaseService.getKpis(projectId) is
// already 5s-cached, so re-registering per tick would be redundant work —
// we only need to register/deregister when the *set* of projects changes,
// not when their KPI values change.

import com.nexus.NeuroForge.models.project.Project;
import com.nexus.NeuroForge.repositories.project.ProjectRepository;
import com.nexus.NeuroForge.services.releases.ReleaseService;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ProjectGaugeRegistrar {

    private static final Logger log = LoggerFactory.getLogger(ProjectGaugeRegistrar.class);

    @Autowired private MeterRegistry meterRegistry;
    @Autowired private ReleaseService releaseService;
    @Autowired private ProjectRepository projectRepository;

    // projectId -> the Meter.Ids this class registered for it, so they can
    // be individually removed if the project disappears. Not just a
    // Set<Long> "registered" flag — MeterRegistry.remove() needs the exact
    // Meter.Id, and rebuilding it from scratch to remove it risks a
    // mismatch (e.g. project name changed) that leaves the old series
    // orphaned in the registry.
    private final Map<Long, List<Meter.Id>> registeredMeterIds = new ConcurrentHashMap<>();

    // Same cadence as the KPI cache (see ReleaseService.KPI_CACHE_MS) would
    // be pointless to beat, but project creation/deletion is rare compared
    // to KPI changes, so 60s (vs the 30s health/alert cadence) is plenty
    // responsive without adding scheduler load.
    @Scheduled(fixedRate = 60000)
    public void syncProjectGauges() {
        List<Project> projects = projectRepository.findAll();
        Set<Long> liveProjectIds = ConcurrentHashMap.newKeySet();

        for (Project project : projects) {
            liveProjectIds.add(project.getId());
            registeredMeterIds.computeIfAbsent(project.getId(), id -> registerGaugesFor(project));
        }

        // Deregister gauges for projects that no longer exist (cascade
        // delete removes releases/pipelines with the project, but nothing
        // tells Micrometer to drop the now-stale cached series).
        registeredMeterIds.keySet().removeIf(projectId -> {
            if (liveProjectIds.contains(projectId)) return false;
            List<Meter.Id> ids = registeredMeterIds.get(projectId);
            if (ids != null) {
                ids.forEach(meterRegistry::remove);
                log.info("Deregistered gauges for deleted project {}", projectId);
            }
            return true;
        });
    }

    private List<Meter.Id> registerGaugesFor(Project project) {
        Long projectId = project.getId();
        Tags tags = Tags.of("project_id", String.valueOf(projectId), "project_name", project.getName());

        Meter.Id uptime = Gauge.builder("neuroforge_release_uptime_percent", releaseService,
                        s -> s.getKpis(projectId).uptimePercent)
                .description("Uptime percentage for this project — real, from HealthCheckResult polls when a monitor is configured; falls back to a rollback-derived estimate otherwise")
                .tags(tags)
                .register(meterRegistry)
                .getId();

        Meter.Id mttr = Gauge.builder("neuroforge_release_mttr_minutes", releaseService,
                        s -> s.getKpis(projectId).mttrMinutes)
                .description("Mean time to recovery in minutes across this project's rolled-back releases")
                .tags(tags)
                .register(meterRegistry)
                .getId();

        Meter.Id releasesThisMonth = Gauge.builder("neuroforge_releases_this_month", releaseService,
                        s -> s.getKpis(projectId).releasesThisMonth)
                .description("Count of releases cut this calendar month for this project")
                .tags(tags)
                .register(meterRegistry)
                .getId();

        Meter.Id rolledBack = Gauge.builder("neuroforge_releases_rolled_back_total", releaseService,
                        s -> s.getKpis(projectId).rolledBackReleases)
                .description("Total number of this project's releases that have been rolled back")
                .tags(tags)
                .register(meterRegistry)
                .getId();

        log.info("Registered gauges for project {} ({})", projectId, project.getName());
        return List.of(uptime, mttr, releasesThisMonth, rolledBack);
    }
}
