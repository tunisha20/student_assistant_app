package com.example.studentassistantappv1.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class SyllabusModel {
    @SerializedName("id") private long id;
    @SerializedName("subject_name") private String subjectName;
    @SerializedName("pdf_uri") private String pdfUri;
    @SerializedName("topics_json") private List<Topic> topics; // এটিই JSON কলাম

    public static class Topic {
        public String name;
        public String subtitle;
        public int status; // 0 = Completed, 1 = In Progress

        public Topic(String name, String subtitle, int status) {
            this.name = name;
            this.subtitle = subtitle;
            this.status = status;
        }
    }

    public long getId() { return id; }
    public String getSubjectName() { return subjectName; }
    public String getPdfUri() { return pdfUri; }
    public List<Topic> getTopics() { return topics; }
}