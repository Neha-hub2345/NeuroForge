package com.nexus.NeuroForge.repositories.sprint;

import com.nexus.NeuroForge.models.sprint.Sprint;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SprintRepository extends JpaRepository<Sprint, Long> {
    List<Sprint> findByProjectId(Long projectId);
    
}