package com.example.fblaapp.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/**
 * Data Access Object for Event operations.
 */
@Dao
public interface EventDao {

    /**
     * Get all events ordered by start time (ascending).
     * Returns LiveData for automatic UI updates.
     */
    @Query("SELECT * FROM events ORDER BY startTimeMillis ASC")
    LiveData<List<EventEntity>> getAllEventsOrderedByStart();

    /**
     * Get all events as a regular list (for non-LiveData use cases).
     */
    @Query("SELECT * FROM events ORDER BY startTimeMillis ASC")
    List<EventEntity> getAllEventsSync();

    /**
     * Get events within a date range.
     */
    @Query("SELECT * FROM events WHERE startTimeMillis >= :startMillis AND startTimeMillis <= :endMillis ORDER BY startTimeMillis ASC")
    LiveData<List<EventEntity>> getEventsInRange(long startMillis, long endMillis);

    /**
     * Get events for a specific day.
     */
    @Query("SELECT * FROM events WHERE startTimeMillis >= :dayStartMillis AND startTimeMillis < :dayEndMillis ORDER BY startTimeMillis ASC")
    List<EventEntity> getEventsForDay(long dayStartMillis, long dayEndMillis);

    /**
     * Get a single event by ID.
     */
    @Query("SELECT * FROM events WHERE id = :eventId LIMIT 1")
    EventEntity getEventById(long eventId);

    /**
     * Insert a new event.
     * @return the row ID of the newly inserted event.
     */
    @Insert
    long insert(EventEntity event);

    /**
     * Update an existing event.
     * @return the number of rows updated.
     */
    @Update
    int update(EventEntity event);

    /**
     * Delete an event.
     * @return the number of rows deleted.
     */
    @Delete
    int delete(EventEntity event);

    /**
     * Delete an event by ID.
     */
    @Query("DELETE FROM events WHERE id = :eventId")
    int deleteById(long eventId);

    /**
     * Get count of all events.
     */
    @Query("SELECT COUNT(*) FROM events")
    int getEventCount();
}
