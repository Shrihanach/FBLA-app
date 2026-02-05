package com.example.fblaapp.data;

import android.content.Context;

import java.util.List;

/**
 * Repository for Reminder operations.
 * All users (MEMBER and OFFICER) can set reminders.
 */
public class ReminderRepository {

    private final ReminderDao reminderDao;
    private final AuthRepository authRepository;

    private static volatile ReminderRepository INSTANCE;

    private ReminderRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.reminderDao = db.reminderDao();
        this.authRepository = AuthRepository.getInstance(context);
    }

    /**
     * Get singleton instance of ReminderRepository.
     */
    public static ReminderRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (ReminderRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ReminderRepository(context);
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Get reminder for a specific user and event.
     */
    public ReminderEntity getReminderForEvent(long userId, long eventId) {
        return reminderDao.getByUserAndEvent(userId, eventId);
    }

    /**
     * Get all enabled reminders for a user.
     */
    public List<ReminderEntity> getEnabledRemindersForUser(long userId) {
        return reminderDao.getEnabledRemindersForUser(userId);
    }

    /**
     * Get reminder by ID.
     */
    public ReminderEntity getReminderById(long reminderId) {
        return reminderDao.getById(reminderId);
    }

    /**
     * Set or update a reminder for an event.
     * @return The created or updated ReminderEntity.
     */
    public ReminderEntity setReminder(long eventId, long userId, long remindAtMillis, boolean enabled) {
        ReminderEntity existing = reminderDao.getByUserAndEvent(userId, eventId);

        if (existing != null) {
            existing.setRemindAtMillis(remindAtMillis);
            existing.setEnabled(enabled);
            reminderDao.update(existing);
            return existing;
        } else {
            ReminderEntity newReminder = new ReminderEntity(eventId, userId, remindAtMillis, enabled);
            long id = reminderDao.insert(newReminder);
            newReminder.setId(id);
            return newReminder;
        }
    }

    /**
     * Disable a reminder.
     */
    public void disableReminder(long reminderId) {
        ReminderEntity reminder = reminderDao.getById(reminderId);
        if (reminder != null) {
            reminder.setEnabled(false);
            reminderDao.update(reminder);
        }
    }

    /**
     * Delete a reminder.
     */
    public void deleteReminder(long reminderId) {
        ReminderEntity reminder = reminderDao.getById(reminderId);
        if (reminder != null) {
            reminderDao.delete(reminder);
        }
    }

    /**
     * Delete all reminders for an event.
     */
    public void deleteRemindersForEvent(long eventId) {
        reminderDao.deleteByEventId(eventId);
    }
}
