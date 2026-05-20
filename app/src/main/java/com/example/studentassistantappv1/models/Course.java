package com.example.studentassistantappv1.models;

public class Course {
    private String name;
    private int credits;
    private String grade;
    private double points;

    public Course(String name, int credits, String grade, double points) {
        this.name = name;
        this.credits = credits;
        this.grade = grade;
        this.points = points;
    }

    // Getters
    public String getName() { return name; }
    public int getCredits() { return credits; }
    public String getGrade() { return grade; }
    public double getPoints() { return points; }
}