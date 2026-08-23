// ObservabilityController.java — [M4][Jashanpreet]
//
// Read-only proxy in front of PrometheusQueryService and (as of the
// /logs endpoint) LogSearchService. Exists so the Releases & Monitoring
// dashboard can show a "live" strip that's visibly sourced from the same
// Prometheus/Grafana/ELK pipeline as step 6 — as opposed to
// ReleaseController's /kpi, which is DB-computed and cached for 60s.
// Same auth tier as the other read endpoints on ReleaseController: any
// authenticated project member can view it.
package com.nexus.NeuroForge.controllers.observability;

import com.nexus.NeuroForge.dto.observability.LiveHealthDTO;
import com.nexus.NeuroForge.dto.observability.LogLineDTO;
import com.nexus.NeuroForge.services.observability.LogSearchService;
import com.nexus.NeuroForge.services.observability.PrometheusQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/observability")
public class ObservabilityController {

    @Autowired
    private PrometheusQueryService prometheusQueryService;

    @Autowired
    private LogSearchService logSearchService;

    @GetMapping("/live")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LiveHealthDTO> getLive(@RequestParam Long projectId) {
        String tag = "project_id=\"" + projectId + "\"";

        boolean scrapeUp = prometheusQueryService.isBackendTargetUp();
        Double uptime = prometheusQueryService
                .queryScalar("neuroforge_release_uptime_percent{" + tag + "}")
                .orElse(null);
        Double mttr = prometheusQueryService
                .queryScalar("neuroforge_release_mttr_minutes{" + tag + "}")
                .orElse(null);
        Double releasesThisMonth = prometheusQueryService
                .queryScalar("neuroforge_releases_this_month{" + tag + "}")
                .orElse(null);
        Double rolledBack = prometheusQueryService
                .queryScalar("neuroforge_releases_rolled_back_total{" + tag + "}")
                .orElse(null);

        LiveHealthDTO dto = new LiveHealthDTO(
                scrapeUp, uptime, mttr, releasesThisMonth, rolledBack,
                System.currentTimeMillis()
        );
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/logs")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LogLineDTO>> getRecentLogs(
            @RequestParam Long projectId,
            @RequestParam(defaultValue = "20") int limit) {
        int cappedLimit = Math.min(Math.max(limit, 1), 50);
        return ResponseEntity.ok(logSearchService.recentLogsForProject(String.valueOf(projectId), cappedLimit));
    }
}
