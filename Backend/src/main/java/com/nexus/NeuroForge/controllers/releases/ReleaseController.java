// ReleaseController.java — [M4][Jashanpreet]
package com.nexus.NeuroForge.controllers.releases;

import com.nexus.NeuroForge.dto.release.CreateReleaseRequest;
import com.nexus.NeuroForge.dto.release.ReleaseDetailDTO;
import com.nexus.NeuroForge.dto.release.ReleaseKpiDTO;
import com.nexus.NeuroForge.dto.release.ReleaseResponse;
import com.nexus.NeuroForge.models.interfaces.DeploymentEnvironment;
import com.nexus.NeuroForge.services.releases.ReleaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/releases")
public class ReleaseController {

    @Autowired private ReleaseService releaseService;

    // CHANGED (M4 step 8): restricted to ADMIN/PROJECT_MANAGER/
    // DEVOPS_ENGINEER — same tier as ProjectIntegrationController's
    // connect()/regenerate-secret and AlertController's rule create/update,
    // plus DEVOPS_ENGINEER since cutting a release is itself a deploy
    // operation. DEVELOPER/TESTER can read release state (see the GETs
    // below) but shouldn't be able to push a release live.
    //
    // CHANGED: was `public Release createRelease(...)`. Returning the raw
    // entity meant Jackson had to serialize release.getDeployment(), and
    // Deployment.release (the mappedBy inverse side) points right back —
    // Release -> Deployment -> Release -> ... until it blows the stack.
    // toResponse() flattens it to plain fields, same shape getHistory()
    // already returns, so nothing on the frontend needs to change.
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER', 'DEVOPS_ENGINEER')")
    public ReleaseResponse createRelease(@RequestBody CreateReleaseRequest request) {
        return releaseService.toResponse(releaseService.createRelease(request));
    }

    // CHANGED: now requires ?projectId= — history is scoped per project so
    // one project's dashboard never shows another project's releases.
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<ReleaseResponse> getHistory(@RequestParam Long projectId) {
        return releaseService.getHistory(projectId);
    }

    // CHANGED: same project scoping for KPIs.
    @GetMapping("/kpi")
    @PreAuthorize("isAuthenticated()")
    public ReleaseKpiDTO getKpis(@RequestParam Long projectId) {
        return releaseService.getKpis(projectId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ReleaseDetailDTO getDetail(@PathVariable Long id) {
        return releaseService.getDetail(id);
    }

    // CHANGED: same reasoning — this is what EnvironmentHealthPanel calls
    // for all 4 environments on every page load. Now also scoped by
    // ?projectId= so two projects both deploying to, say, STAGING don't
    // show each other's active release. Returning the raw entity meant it
    // 500'd every time there WAS an active release to show, which your
    // frontend's error handling silently displayed as "No active
    // release" — looking identical to the correct empty state.
    @GetMapping("/active/{environment}")
    @PreAuthorize("isAuthenticated()")
    public ReleaseResponse getActiveRelease(
            @PathVariable DeploymentEnvironment environment,
            @RequestParam Long projectId) {
        return releaseService.toResponse(releaseService.getActiveRelease(projectId, environment));
    }

    // CHANGED (M4 step 8): same role tier as createRelease — a rollback is
    // a production-impacting action, not a read. Deliberately NOT
    // hasRole('ADMIN') only (unlike AlertController.deleteRule) — during an
    // actual incident, requiring a PROJECT_MANAGER or DEVOPS_ENGINEER to
    // wait on an admin to be available to roll back a bad release would
    // make the outage worse, not safer.
    @PostMapping("/{id}/rollback")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER', 'DEVOPS_ENGINEER')")
    public ResponseEntity<String> rollbackRelease(@PathVariable Long id) {
        releaseService.rollbackRelease(id);
        return ResponseEntity.ok("Rollback initiated");
    }
}