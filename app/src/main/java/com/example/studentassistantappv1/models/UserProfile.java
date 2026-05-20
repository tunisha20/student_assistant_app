package com.example.studentassistantappv1.models;

// ✅ এই ইম্পোর্টটি অবশ্যই যোগ করতে হবে
import com.google.gson.annotations.SerializedName;

public class UserProfile {
    @SerializedName("user_cgpa")
    private String cgpa;

    @SerializedName("user_focus_time")

    private String focusTime;

    // Getters
    public String getCgpa() {
        return cgpa != null ? cgpa : "0.00";
    }

    public String getFocusTime() {
        return focusTime != null ? focusTime : "0.0";
    }
}