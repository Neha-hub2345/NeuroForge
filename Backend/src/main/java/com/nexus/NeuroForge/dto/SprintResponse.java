package com.nexus.NeuroForge.dto;

public class SprintResponse {
    private Long id;
    private String goal;
    private String dates;
    private Long projectId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getGoal() { return goal; }
    public void setGoal(String goal) { this.goal = goal; }

    public String getDates() { return dates; }
    public void setDates(String dates) { this.dates = dates; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
}