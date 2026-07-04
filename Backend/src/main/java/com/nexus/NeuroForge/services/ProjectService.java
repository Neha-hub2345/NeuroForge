package com.nexus.NeuroForge.services;

import com.nexus.NeuroForge.dto.ProjectRequest;
import com.nexus.NeuroForge.dto.ProjectResponse;
import com.nexus.NeuroForge.models.ProjectStatus;

import java.util.List;

public interface ProjectService {
    ProjectResponse createProject(ProjectRequest req, String creatorUsername);
    List<ProjectResponse> getAll();
    ProjectResponse getById(Long id);
    ProjectResponse assignTeam(Long projectId, Long teamId);
    ProjectResponse updateStatus(Long projectId, ProjectStatus status);
    void deleteProject(Long id);
}
