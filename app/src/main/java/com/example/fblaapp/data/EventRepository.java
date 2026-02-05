package com.example.fblaapp.data;

import android.content.Context;

import androidx.lifecycle.LiveData;

import java.util.List;

/**
 * Repository for Event operations.
 * Enforces officer-only access for create, update, and delete operations.
 */
public class EventRepository {

    private final EventDao eventDao;
    private final AuthRepository authRepository;

    private static volatile EventRepository INSTANCE;

    private EventRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.eventDao = db.eventDao();
        this.authRepository = AuthRepository.getInstance(context);
    }

    /**
     * Get singleton instance of EventRepository.
     */
    public static EventRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (EventRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new EventRepository(context);
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Check if current user is an officer.
     * @return true if logged in user has OFFICER role.
     */
    public boolean isOfficer() {
        UserEntity currentUser = authRepository.getCurrentUser();
        return currentUser != null && UserEntity.ROLE_OFFICER.equals(currentUser.getRole());
    }

    /**
     * Get the current user ID.
     * @return user ID or -1 if not logged in.
     */
    public long getCurrentUserId() {
        UserEntity currentUser = authRepository.getCurrentUser();
        return currentUser != null ? currentUser.getId() : -1;
    }

    /**
     * Get all events ordered by start time.
     * @return LiveData list of all events.
     */
    public LiveData<List<EventEntity>> getAllEventsOrderedByStart() {
        return eventDao.getAllEventsOrderedByStart();
    }

    /**
     * Get all events synchronously.
     * @return List of all events.
     */
    public List<EventEntity> getAllEventsSync() {
        return eventDao.getAllEventsSync();
    }

    /**
     * Get events within a date range.
     */
    public LiveData<List<EventEntity>> getEventsInRange(long startMillis, long endMillis) {
        return eventDao.getEventsInRange(startMillis, endMillis);
    }

    /**
     * Get events for a specific day.
     */
    public List<EventEntity> getEventsForDay(long dayStartMillis, long dayEndMillis) {
        return eventDao.getEventsForDay(dayStartMillis, dayEndMillis);
    }

    /**
     * Get a single event by ID.
     */
    public EventEntity getEventById(long eventId) {
        return eventDao.getEventById(eventId);
    }

    /**
     * Create a new event.
     * Only officers can create events.
     * 
     * @param event The event to create.
     * @return The ID of the newly created event.
     * @throws SecurityException if user is not an officer.
     * @throws IllegalArgumentException if validation fails.
     */
    public long createEvent(EventEntity event) throws SecurityException, IllegalArgumentException {
        // Authorization check
        if (!isOfficer()) {
            throw new SecurityException("Not authorized: Only officers can create events");
        }

        // Validation
        validateEvent(event);

        // Set the creator
        event.setCreatedByUserId(getCurrentUserId());

        return eventDao.insert(event);
    }

    /**
     * Update an existing event.
     * Only officers can update events.
     * 
     * @param event The event to update.
     * @return The number of rows updated.
     * @throws SecurityException if user is not an officer.
     * @throws IllegalArgumentException if validation fails.
     */
    public int updateEvent(EventEntity event) throws SecurityException, IllegalArgumentException {
        // Authorization check
        if (!isOfficer()) {
            throw new SecurityException("Not authorized: Only officers can update events");
        }

        // Validation
        validateEvent(event);

        return eventDao.update(event);
    }

    /**
     * Delete an event.
     * Only officers can delete events.
     * 
     * @param event The event to delete.
     * @return The number of rows deleted.
     * @throws SecurityException if user is not an officer.
     */
    public int deleteEvent(EventEntity event) throws SecurityException {
        // Authorization check
        if (!isOfficer()) {
            throw new SecurityException("Not authorized: Only officers can delete events");
        }

        return eventDao.delete(event);
    }

    /**
     * Delete an event by ID.
     * Only officers can delete events.
     * 
     * @param eventId The ID of the event to delete.
     * @return The number of rows deleted.
     * @throws SecurityException if user is not an officer.
     */
    public int deleteEventById(long eventId) throws SecurityException {
        // Authorization check
        if (!isOfficer()) {
            throw new SecurityException("Not authorized: Only officers can delete events");
        }

        return eventDao.deleteById(eventId);
    }

    /**
     * Validate event data.
     * 
     * @param event The event to validate.
     * @throws IllegalArgumentException if validation fails.
     */
    private void validateEvent(EventEntity event) throws IllegalArgumentException {
        // Title must not be blank
        if (event.getTitle() == null || event.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Event title cannot be blank");
        }

        // End time must be after start time
        if (event.getEndTimeMillis() <= event.getStartTimeMillis()) {
            throw new IllegalArgumentException("End time must be after start time");
        }
    }
}
