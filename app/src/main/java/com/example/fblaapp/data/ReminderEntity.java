package com.example.fblaapp.data;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Reminder entity for Room database.
 * Represents a reminder for an event set by a user.
 */
@Entity(
    tableName = "reminders",
    indices = {
        @Index(value = {"userId", "eventId"}, unique = true),
        @Index(value = {"eventId"})
    },
    foreignKeys = {
        @ForeignKey(
            entity = EventEntity.class,
            parentColumns = "id",
            childColumns = "eventId",
            onDelete = ForeignKey.CASCADE
        ),
        @ForeignKey(
            entity = UserEntity.class,
            parentColumns = "id",
            childColumns = "userId",
            onDelete = ForeignKey.CASCADE
        )
    }
)
public class ReminderEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private long eventId;

    private long userId;

    private long remindAtMillis;

    private boolean enabled;

    // Constructor
    public ReminderEntity(long eventId, long userId, long remindAtMillis, boolean enabled) {
        this.eventId = eventId;
        this.userId = userId;
        this.remindAtMillis = remindAtMillis;
        this.enabled = enabled;
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getEventId() {
        return eventId;
    }

    public void setEventId(long eventId) {
        this.eventId = eventId;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public long getRemindAtMillis() {
        return remindAtMillis;
    }

    public void setRemindAtMillis(long remindAtMillis) {
        this.remindAtMillis = remindAtMillis;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
