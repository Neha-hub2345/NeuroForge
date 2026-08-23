package com.nexus.NeuroForge.repositories.milestone;

import com.nexus.NeuroForge.models.milestone.Milestone;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MilestoneRepository extends JpaRepository<Milestone, Long> {
    List<Milestone> findByProjectId(Long projectId);
}