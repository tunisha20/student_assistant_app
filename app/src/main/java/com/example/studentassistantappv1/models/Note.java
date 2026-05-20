package com.example.studentassistantappv1.models;

public class Note {
    private String subject;
    private String title;
    private String content;
    private String dateTime;
    private String pdfUri; // 🔥 নতুন ফিল্ড: PDF লিংক

    // Constructor আপডেট করা হলো
    public Note(String subject, String title, String content, String dateTime, String pdfUri) {
        this.subject = subject;
        this.title = title;
        this.content = content;
        this.dateTime = dateTime;
        this.pdfUri = pdfUri;
    }

    public String getSubject() { return subject; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getDateTime() { return dateTime; }
    public String getPdfUri() { return pdfUri; } // Getter
}