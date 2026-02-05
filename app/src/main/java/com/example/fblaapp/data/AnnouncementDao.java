package com.example.fblaapp.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

/**
 * Data Access Object for Announcement operations.
 */
@Dao
public interface AnnouncementDao {

    @Query("SELECT * FROM announcements ORDER BY createdAtMillis DESC")
    LiveData<List<AnnouncementEntity>> getAllAnnouncementsLive();

    @Query("SELECT * FROM announcements ORDER BY createdAtMillis DESC")
    List<AnnouncementEntity> getAllAnnouncements();

    @Query("SELECT * FROM announcements ORDER BY createdAtMillis DESC LIMIT :limit")
    List<AnnouncementEntity> getRecentAnnouncements(int limit);

    @Query("SELECT * FROM announcements ORDER BY createdAtMillis DESC LIMIT :limit OFFSET :offset")
    List<AnnouncementEntity> getAnnouncementsPaged(int limit, int offset);

    @Query("SELECT COUNT(*) FROM announcements")
    int getAnnouncementCount();

    @Query("SELECT * FROM announcements WHERE id = :id LIMIT 1")
    AnnouncementEntity getById(long id);

    @Insert
    long insert(AnnouncementEntity announcement);

    @androidx.room.Update
    int update(AnnouncementEntity announcement);

    @Delete
    int delete(AnnouncementEntity announcement);

    @Query("DELETE FROM announcements WHERE id = :id")
    int deleteById(long id);
}
