package com.skill.sync2.skillsync2;

public class Task {
    private int id;
    private String teamId;
    private String creator;
    private String title;
    private String assignedTo;
    private String status;
    private String dueDate;

    public Task(int id, String teamId, String creator, String title, String assignedTo, String status, String dueDate) {
        this.id = id;
        this.teamId = teamId;
        this.creator = creator;
        this.title = title;
        this.assignedTo = assignedTo;
        this.status = status;
        this.dueDate = dueDate;
    }

    public int getId() { return id; }
    public String getTeamId() { return teamId; }
    public String getCreator() { return creator; }
    public String getTitle() { return title; }
    public String getAssignedTo() { return assignedTo; }
    public String getStatus() { return status; }
    public String getDueDate() { return dueDate; }

    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }
    public void setStatus(String status) { this.status = status; }
}