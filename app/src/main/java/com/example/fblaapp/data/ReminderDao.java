package com.example.fblaapp.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/**
 * Data Access Object for Reminder operations.
 */
@Dao
public interface ReminderDao {

    @Query("SELECT * FROM reminders WHERE userId = :userId AND eventId = :eventId LIMIT 1")
    ReminderEntity getByUserAndEvent(long userId, long eventId);

    @Query("SELECT * FROM reminders WHERE userId = :userId AND enabled = 1")
    List<ReminderEntity> getEnabledRemindersForUser(long userId);

    @Query("SELECT * FROM reminders WHERE id = :reminderId LIMIT 1")
    ReminderEntity getById(long reminderId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(ReminderEntity reminder);

    @Update
    int update(ReminderEntity reminder);

    @Delete
    int delete(ReminderEntity reminder);

    @Query("DELETE FROM reminders WHERE eventId = :eventId")
    int deleteByEventId(long eventId);
}
