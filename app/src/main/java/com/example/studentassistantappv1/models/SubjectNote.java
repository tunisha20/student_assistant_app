package com.example.studentassistantappv1.models;

import com.google.gson.annotations.SerializedName;

public class SubjectNote {

    @SerializedName("id") // ✅ সুপাবেসের আইডি কলামের সাথে ম্যাপ করবে
    private long id;

    @SerializedName("subject_name")
    private String title;

    @SerializedName("note_count")
    private String count;

    @SerializedName("pdf_uri")
    private String pdfUri;

    // কনস্ট্রাক্টর (id সহ আপডেট করা হয়েছে)
    public SubjectNote(long id, String title, String count, String pdfUri) {
        this.id = id;
        this.title = title;
        this.count = count;
        this.pdfUri = pdfUri;
    }

    // গেটার মেথডগুলো
    public long getId() {
        return id;
    }

    public String getTitle() {
        return title != null ? title : "Untitled";
    }

    public String getCount() {
        return count != null ? count : "1 note";
    }

    public String getPdfUri() {
        return pdfUri != null ? pdfUri : "";
    }
}