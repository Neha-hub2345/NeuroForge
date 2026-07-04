package com.nexus.NeuroForge.dto;

import com.nexus.NeuroForge.models.ProjectStatus;

public class ProjectStatusUpdateRequest {
    private ProjectStatus status;

    public ProjectStatus getStatus() {
        return status;
    }

    public void setStatus(ProjectStatus status) {
        this.status = status;
    }
}