package com.nexus.NeuroForge.models;

import java.util.ArrayList;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Sprint {
    
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String goal;
    private String dates;

    @ManyToOne
    @JoinColumn(name = "project_id")
    private Project project;

    @OneToMany(mappedBy = "sprint", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Task> tasks = new ArrayList<>();

    public Sprint() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getGoal() { return goal; }
    public void setGoal(String goal) { this.goal = goal; }

    public String getDates() { return dates; }
    public void setDates(String dates) { this.dates = dates; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
}
