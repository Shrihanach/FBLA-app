package com.example.fblaapp.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Announcement entity for Room database.
 * Represents an announcement posted by an officer.
 */
@Entity(
    tableName = "announcements",
    foreignKeys = @ForeignKey(
        entity = UserEntity.class,
        parentColumns = "id",
        childColumns = "createdByUserId",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {@Index("createdByUserId"), @Index("createdAtMillis")}
)
public class AnnouncementEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @NonNull
    private String title;

    @NonNull
    private String content;

    private long createdByUserId;

    private String authorName;

    private long createdAtMillis;

    // Constructor
    public AnnouncementEntity(@NonNull String title, @NonNull String content, 
                               long createdByUserId, String authorName, long createdAtMillis) {
        this.title = title;
        this.content = content;
        this.createdByUserId = createdByUserId;
        this.authorName = authorName;
        this.createdAtMillis = createdAtMillis;
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    public void setTitle(@NonNull String title) {
        this.title = title;
    }

    @NonNull
    public String getContent() {
        return content;
    }

    public void setContent(@NonNull String content) {
        this.content = content;
    }

    public long getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(long createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public long getCreatedAtMillis() {
        return createdAtMillis;
    }

    public void setCreatedAtMillis(long createdAtMillis) {
        this.createdAtMillis = createdAtMillis;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }
}
