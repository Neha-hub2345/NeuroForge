package com.nexus.NeuroForge.repositories.team;

import com.nexus.NeuroForge.models.team.Team;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team,Long> {
    boolean existsByName(String name);
}
