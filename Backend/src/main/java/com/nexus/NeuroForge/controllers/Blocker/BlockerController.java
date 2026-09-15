package com.nexus.NeuroForge.controllers.Blocker;

import com.nexus.NeuroForge.models.blocker.Blocker;
import com.nexus.NeuroForge.services.blocker.BlockerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sprints/{sprintId}/blockers")
public class BlockerController {

    @Autowired
    private BlockerService blockerService;

    @GetMapping
    public List<Blocker> getBlockers(@PathVariable Long sprintId) {
        return blockerService.getBlockersBySprint(sprintId);
    }

    @PostMapping
    public Blocker raiseBlocker(@PathVariable Long sprintId, @RequestBody Blocker request,@AuthenticationPrincipal Jwt jwt) {
        return blockerService.raiseBlocker(sprintId, request,jwt);
    }

    @PutMapping("/{blockerId}/resolve")
    public Blocker resolveBlocker(@PathVariable Long sprintId, @PathVariable Long blockerId,@AuthenticationPrincipal Jwt jwt) {
        return blockerService.resolveBlocker(sprintId, blockerId,jwt);
    }
}