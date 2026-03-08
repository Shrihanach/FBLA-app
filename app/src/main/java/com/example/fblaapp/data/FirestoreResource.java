package com.example.fblaapp.data;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

/**
 * Model class representing a resource document in Firestore.
 * Collection: resources
 */
public class FirestoreResource {

    @DocumentId
    private String id;
    
    private String title;
    private String description;
    private String category;
    private String fileUrl;
    private String fileType;
    private String uploadedBy;
    
    @ServerTimestamp
    private Date uploadedAt;

    // Empty constructor required for Firestore
    public FirestoreResource() {}

    public FirestoreResource(String title, String description, String category, 
                            String fileUrl, String fileType, String uploadedBy) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.fileUrl = fileUrl;
        this.fileType = fileType;
        this.uploadedBy = uploadedBy;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public Date getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(Date uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    /**
     * Get a display-friendly file type icon indicator
     */
    public String getFileTypeIcon() {
        if (fileType == null) return "📄";
        
        switch (fileType.toLowerCase()) {
            case "pdf":
                return "📕";
            case "doc":
            case "docx":
                return "📘";
            case "ppt":
            case "pptx":
                return "📙";
            case "xls":
            case "xlsx":
                return "📗";
            case "jpg":
            case "jpeg":
            case "png":
            case "gif":
                return "🖼️";
            case "mp4":
            case "avi":
            case "mov":
                return "🎬";
            case "mp3":
            case "wav":
                return "🎵";
            case "zip":
            case "rar":
                return "📦";
            case "link":
                return "🔗";
            default:
                return "📄";
        }
    }
}
