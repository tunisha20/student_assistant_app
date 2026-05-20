package com.example.studentassistantappv1.models;

import com.google.gson.annotations.SerializedName;

public class Routine {

    @SerializedName("id")
    private long id; // সুপাবেস থেকে আসা প্রাইমারি কি

    @SerializedName("subject")
    private String subject;

    @SerializedName("teacher")
    private String teacher;

    @SerializedName("start_time")
    private String startTime; // Format: "HH:mm"

    @SerializedName("end_time")
    private String endTime;   // Format: "HH:mm"

    @SerializedName("room")
    private String room;

    @SerializedName("class_type")
    private String type;      // Theory or Lab

    @SerializedName("is_cancelled")
    private boolean isCancelled;

    @SerializedName("day_of_week")
    private String dayOfWeek; // SAT, SUN, MON...

    // ১. সুপাবেস থেকে ডাটা লোড করার সময় এই কনস্ট্রাক্টরটি কাজে লাগবে
    public Routine(long id, String subject, String teacher, String startTime, String endTime, String room, String type, boolean isCancelled, String dayOfWeek) {
        this.id = id;
        this.subject = subject;
        this.teacher = teacher;
        this.startTime = startTime;
        this.endTime = endTime;
        this.room = room;
        this.type = type;
        this.isCancelled = isCancelled;
        this.dayOfWeek = dayOfWeek;
    }

    // ২. নতুন ডাটা লোকালি তৈরি করার জন্য কনস্ট্রাক্টর
    public Routine(String subject, String teacher, String startTime, String endTime, String room, String type, String dayOfWeek) {
        this.subject = subject;
        this.teacher = teacher;
        this.startTime = startTime;
        this.endTime = endTime;
        this.room = room;
        this.type = type;
        this.dayOfWeek = dayOfWeek;
        this.isCancelled = false;
    }

    // --- GETTERS & SETTERS ---
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getSubject() { return subject; }
    public String getTeacher() { return teacher; }

    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }

    public String getRoom() { return room; }
    public String getType() { return type; }

    public boolean isCancelled() { return isCancelled; }
    public void setCancelled(boolean cancelled) { isCancelled = cancelled; }

    public String getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }
}