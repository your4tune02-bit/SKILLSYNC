package com.skill.sync2.skillsync2;

public class Student {
    private String name;
    private String skills;
    private String bio;

    public Student(String name, String skills, String bio) {
        this.name = name;
        this.skills = skills;
        this.bio = bio;
    }

    public String getName() { return name; }
    public String getSkills() { return skills; }
    public String getBio() { return bio; }
}