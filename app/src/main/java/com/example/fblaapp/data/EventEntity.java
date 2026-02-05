package com.example.fblaapp.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Event entity for Room database.
 * Represents an event in the FBLA Connect app.
 */
@Entity(
    tableName = "events",
    foreignKeys = @ForeignKey(
        entity = UserEntity.class,
        parentColumns = "id",
        childColumns = "createdByUserId",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {@Index("createdByUserId"), @Index("startTimeMillis")}
)
public class EventEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @NonNull
    private String title;

    @Nullable
    private String description;

    @Nullable
    private String location;

    private long startTimeMillis;

    private long endTimeMillis;

    private long createdByUserId;

    // Constructor
    public EventEntity(@NonNull String title, @Nullable String description, 
                       @Nullable String location, long startTimeMillis, 
                       long endTimeMillis, long createdByUserId) {
        this.title = title;
        this.description = description;
        this.location = location;
        this.startTimeMillis = startTimeMillis;
        this.endTimeMillis = endTimeMillis;
        this.createdByUserId = createdByUserId;
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

    @Nullable
    public String getDescription() {
        return description;
    }

    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    @Nullable
    public String getLocation() {
        return location;
    }

    public void setLocation(@Nullable String location) {
        this.location = location;
    }

    public long getStartTimeMillis() {
        return startTimeMillis;
    }

    public void setStartTimeMillis(long startTimeMillis) {
        this.startTimeMillis = startTimeMillis;
    }

    public long getEndTimeMillis() {
        return endTimeMillis;
    }

    public void setEndTimeMillis(long endTimeMillis) {
        this.endTimeMillis = endTimeMillis;
    }

    public long getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(long createdByUserId) {
        this.createdByUserId = createdByUserId;
    }
}
