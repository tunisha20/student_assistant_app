package com.example.studentassistantappv1.models;

import com.google.gson.annotations.SerializedName;

public class Task {
    @SerializedName("id")
    private long id;

    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    @SerializedName("priority")
    private String priority;

    // আপনার DB-তে কলামের নাম 'subject', তাই এখানে 'subject' ই দিতে হবে
    @SerializedName("subject")
    private String category;

    // আপনার DB-তে 'due_date' নামে কলাম নেই, আপনি 'created_at' ব্যবহার করতে পারেন
    // অথবা SQL-এ 'due_date' কলামটি অ্যাড করতে হবে।
    // যদি DB-তে কলামের নাম 'due_date' হয় তবেই এটি কাজ করবে।
    @SerializedName("due_date")
    private String date;

    @SerializedName("is_completed")
    private boolean isCompleted;

    // কনস্ট্রাক্টর (সুপাবেস থেকে ডাটা লোড করার জন্য)
    public Task(long id, String title, String category, String date, String priority, String description, boolean isCompleted) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.date = date;
        this.priority = priority;
        this.description = description;
        this.isCompleted = isCompleted;
    }

    // Getters
    public long getId() { return id; }
    public String getTitle() { return title; }
    public String getCategory() { return category != null ? category : "General"; }
    public String getDate() { return date != null ? date : "No Date"; }
    public boolean isCompleted() { return isCompleted; }

    public void setCompleted(boolean completed) { isCompleted = completed; }

}