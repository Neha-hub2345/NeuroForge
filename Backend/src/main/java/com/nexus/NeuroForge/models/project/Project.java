package com.nexus.NeuroForge.models.project;

import com.nexus.NeuroForge.models.task.Task;
import com.nexus.NeuroForge.models.team.Team;
import com.nexus.NeuroForge.models.user.User;
import com.nexus.NeuroForge.models.interfaces.ProjectStatus;
import com.nexus.NeuroForge.models.pipeline.Pipeline;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Project {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;

    @Enumerated(EnumType.STRING)
    private ProjectStatus status;

    @ManyToOne
    @JoinColumn(name="team_id")
    private Team team;

    @ManyToOne
    @JoinColumn(name = "manager_id")
    private User manager;

    private LocalDate createdAt;

// Add this to cascade delete pipelines tied to the project
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Pipeline> pipelines = new ArrayList<>();

    // Add this to cascade delete all tasks (including backlog tasks) tied to the project
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Task> tasks = new ArrayList<>();

    public Project(){}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public void setStatus(ProjectStatus status) {
        this.status = status;
    }

    public LocalDate getCreatedAt() {
        return createdAt;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public User getManager() {
        return manager;
    }

    public void setManager(User manager) {
        this.manager = manager;
    }

    public void setCreatedAt(LocalDate now) {
        createdAt=now;
    }
}
