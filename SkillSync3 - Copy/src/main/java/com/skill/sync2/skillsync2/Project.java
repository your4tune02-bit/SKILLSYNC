package com.skill.sync2.skillsync2;

public class Project {
    private int id;
    private String ownerName;
    private String title;
    private String description;
    private String requiredRole;
    private String responsibilities;
    private String dueDate;

    public Project(int id, String ownerName, String title, String description, String requiredRole, String responsibilities, String dueDate) {
        this.id = id;
        this.ownerName = ownerName;
        this.title = title;
        this.description = description;
        this.requiredRole = requiredRole;
        this.responsibilities = responsibilities;
        this.dueDate = dueDate;
    }

    public int getId() { return id; }
    public String getOwnerName() { return ownerName; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getRequiredRole() { return requiredRole; }
    public String getResponsibilities() { return responsibilities; }
    public String getDueDate() { return dueDate; }
}