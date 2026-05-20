package com.example.studentassistantappv1.models;

public class SyllabusTopic {
    private String subjectName;
    private String pdfUri; // PDF ফাইলের লোকেশন (Link)

    // Constructor
    public SyllabusTopic(String subjectName, String pdfUri) {
        this.subjectName = subjectName;
        this.pdfUri = pdfUri;
    }

    // Getter Methods
    public String getSubjectName() {
        return subjectName;
    }

    public String getPdfUri() {
        return pdfUri;
    }

    // Setter Methods (ডেটা আপডেট করার জন্য)
    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public void setPdfUri(String pdfUri) {
        this.pdfUri = pdfUri;
    }
}