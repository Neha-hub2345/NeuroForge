package com.nexus.NeuroForge.controllers.sprint;

import com.nexus.NeuroForge.dto.sprint.SprintProgressDTO;
import com.nexus.NeuroForge.services.sprint.SprintProgressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sprint-progress")
public class SprintProgressController {

    @Autowired
    private SprintProgressService sprintProgressService;

    @GetMapping("/{sprintId}")
    public ResponseEntity<SprintProgressDTO> getProgress(@PathVariable Long sprintId) {
        return ResponseEntity.ok(sprintProgressService.getSprintProgress(sprintId));
    }
}