package com.skill.sync2.skillsync2;

public class PortfolioItem {
    private int id;
    private String username;
    private String fileName;
    private String filePath;

    public PortfolioItem(int id, String username, String fileName, String filePath) {
        this.id = id;
        this.username = username;
        this.fileName = fileName;
        this.filePath = filePath;
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getFileName() { return fileName; }
    public String getFilePath() { return filePath; }
}