package com.example.fblaapp.data;

import android.content.Context;

import androidx.lifecycle.LiveData;

import java.util.List;

/**
 * Repository for Announcement operations.
 * Only officers can create and delete announcements.
 */
public class AnnouncementRepository {

    private final AnnouncementDao announcementDao;
    private final AuthRepository authRepository;

    private static volatile AnnouncementRepository INSTANCE;

    private AnnouncementRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.announcementDao = db.announcementDao();
        this.authRepository = AuthRepository.getInstance(context);
    }

    /**
     * Get singleton instance of AnnouncementRepository.
     */
    public static AnnouncementRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AnnouncementRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new AnnouncementRepository(context);
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Check if current user is an officer.
     */
    public boolean isOfficer() {
        UserEntity currentUser = authRepository.getCurrentUser();
        return currentUser != null && currentUser.isOfficer();
    }

    /**
     * Get current user ID.
     */
    public long getCurrentUserId() {
        UserEntity currentUser = authRepository.getCurrentUser();
        return currentUser != null ? currentUser.getId() : -1;
    }

    /**
     * Get all announcements as LiveData.
     */
    public LiveData<List<AnnouncementEntity>> getAllAnnouncementsLive() {
        return announcementDao.getAllAnnouncementsLive();
    }

    /**
     * Get recent announcements (limited).
     */
    public List<AnnouncementEntity> getRecentAnnouncements(int limit) {
        return announcementDao.getRecentAnnouncements(limit);
    }

    /**
     * Get announcements with pagination.
     */
    public List<AnnouncementEntity> getAnnouncementsPaged(int limit, int offset) {
        return announcementDao.getAnnouncementsPaged(limit, offset);
    }

    /**
     * Get total announcement count.
     */
    public int getAnnouncementCount() {
        return announcementDao.getAnnouncementCount();
    }

    /**
     * Get current user name.
     */
    public String getCurrentUserName() {
        UserEntity currentUser = authRepository.getCurrentUser();
        return currentUser != null ? currentUser.getName() : "Unknown";
    }

    /**
     * Create a new announcement.
     * Only officers can create announcements.
     */
    public long createAnnouncement(String title, String content) throws SecurityException {
        if (!isOfficer()) {
            throw new SecurityException("Only officers can post announcements");
        }

        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title cannot be empty");
        }

        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Content cannot be empty");
        }

        AnnouncementEntity announcement = new AnnouncementEntity(
                title.trim(),
                content.trim(),
                getCurrentUserId(),
                getCurrentUserName(),
                System.currentTimeMillis()
        );

        return announcementDao.insert(announcement);
    }

    /**
     * Update an existing announcement.
     * Only the officer who created it can edit.
     */
    public int updateAnnouncement(long announcementId, String title, String content) throws SecurityException {
        if (!isOfficer()) {
            throw new SecurityException("Only officers can edit announcements");
        }

        AnnouncementEntity announcement = announcementDao.getById(announcementId);
        if (announcement == null) {
            throw new IllegalArgumentException("Announcement not found");
        }

        // Only the creator can edit
        if (announcement.getCreatedByUserId() != getCurrentUserId()) {
            throw new SecurityException("You can only edit your own announcements");
        }

        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title cannot be empty");
        }

        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Content cannot be empty");
        }

        announcement.setTitle(title.trim());
        announcement.setContent(content.trim());

        return announcementDao.update(announcement);
    }

    /**
     * Check if current user can edit the announcement.
     */
    public boolean canEdit(AnnouncementEntity announcement) {
        if (!isOfficer() || announcement == null) {
            return false;
        }
        return announcement.getCreatedByUserId() == getCurrentUserId();
    }

    /**
     * Delete an announcement.
     * Only officers can delete announcements.
     */
    public int deleteAnnouncement(long announcementId) throws SecurityException {
        if (!isOfficer()) {
            throw new SecurityException("Only officers can delete announcements");
        }

        return announcementDao.deleteById(announcementId);
    }

    /**
     * Get announcement by ID.
     */
    public AnnouncementEntity getAnnouncementById(long id) {
        return announcementDao.getById(id);
    }
}
