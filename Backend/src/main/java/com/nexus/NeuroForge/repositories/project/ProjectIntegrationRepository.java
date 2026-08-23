package com.nexus.NeuroForge.repositories.project;

import com.nexus.NeuroForge.models.project.ProjectIntegration;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ProjectIntegrationRepository extends JpaRepository<ProjectIntegration, Long> {
    Optional<ProjectIntegration> findByProject_Id(Long projectId);
}