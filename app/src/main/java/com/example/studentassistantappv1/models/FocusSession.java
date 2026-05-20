package com.example.studentassistantappv1.models;
public class FocusSession {
    private String user_id;
    private int duration_minutes;
    private int coins_earned;

    public FocusSession(String user_id, int duration_minutes, int coins_earned) {
        this.user_id = user_id;
        this.duration_minutes = duration_minutes;
        this.coins_earned = coins_earned;
    }
}