package com.example.studentassistantappv1.models;

import com.google.gson.annotations.SerializedName;

public class SemesterRecord {

    // @SerializedName এর ভেতরে সুপাবেস টেবিলের হুবহু কলামের নাম দিন
    @SerializedName("semester_name")
    private String name;

    @SerializedName("courses_count") // আপনার টেবিলের কলামের নাম অনুযায়ী পরিবর্তন করুন
    private int courses;

    @SerializedName("credits")
    private int credits;

    @SerializedName("gpa")
    private double gpa;

    @SerializedName("grade_label")
    private String grade;

    public SemesterRecord(String name, int courses, int credits, double gpa, String grade) {
        this.name = name;
        this.courses = courses;
        this.credits = credits;
        this.gpa = gpa;
        this.grade = grade;
    }

    public String getName() { return name; }

    public String getDetails() {
        return courses + " Courses • " + credits + " Credits";
    }

    public String getGpa() {
        return String.format("%.2f", gpa);
    }

    public String getGradeLabel() {
        return grade;
    }

    public int getCourses() { return courses; }
    public int getCredits() { return credits; }
    public String getGrade() { return grade; }
}