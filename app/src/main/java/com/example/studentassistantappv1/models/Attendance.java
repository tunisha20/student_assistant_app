package com.example.studentassistantappv1.models;

import com.google.gson.annotations.SerializedName;

public class Attendance {

    @SerializedName("id")
    private int id;

    @SerializedName("subject_name")
    private String subjectName;

    @SerializedName("total_classes")
    private int totalClasses;

    @SerializedName("present_classes")
    private int presentClasses;

    // Constructor
    public Attendance(String subjectName, int totalClasses, int presentClasses) {
        this.subjectName = subjectName;
        this.totalClasses = totalClasses;
        this.presentClasses = presentClasses;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public int getTotalClasses() {
        return totalClasses;
    }

    public void setTotalClasses(int totalClasses) {
        this.totalClasses = totalClasses;
    }

    public int getPresentClasses() {
        return presentClasses;
    }

    public void setPresentClasses(int presentClasses) {
        this.presentClasses = presentClasses;
    }
}