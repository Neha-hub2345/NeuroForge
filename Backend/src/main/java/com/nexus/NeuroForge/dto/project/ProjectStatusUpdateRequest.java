package com.nexus.NeuroForge.dto.project;

import com.nexus.NeuroForge.models.interfaces.ProjectStatus;

public class ProjectStatusUpdateRequest {
    private ProjectStatus status;

    public ProjectStatus getStatus() {
        return status;
    }

    public void setStatus(ProjectStatus status) {
        this.status = status;
    }
}