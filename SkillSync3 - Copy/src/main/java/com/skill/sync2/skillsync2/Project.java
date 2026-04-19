package com.skill.sync2.skillsync2;

public class Project {
    private int id;
    private String ownerName;
    private String title;
    private String description;
    private String requiredRole;
    private String responsibilities;
    private String dueDate;
    private String status; // "ACTIVE", "DONE", or "EXPIRED"

    public Project(int id, String ownerName, String title, String description, String requiredRole,
            String responsibilities, String dueDate, String status) {
        this.id = id;
        this.ownerName = ownerName;
        this.title = title;
        this.description = description;
        this.requiredRole = requiredRole;
        this.responsibilities = responsibilities;
        this.dueDate = dueDate;
        this.status = (status != null) ? status : "ACTIVE";
    }

    public int getId() {
        return id;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getRequiredRole() {
        return requiredRole;
    }

    public String getResponsibilities() {
        return responsibilities;
    }

    public String getDueDate() {
        return dueDate;
    }

    public String getStatus() {
        return status;
    }

    public boolean isActive() {
        return "ACTIVE".equals(status);
    }

    public boolean isDone() {
        return "DONE".equals(status);
    }

    public boolean isExpired() {
        return "EXPIRED".equals(status);
    }
}