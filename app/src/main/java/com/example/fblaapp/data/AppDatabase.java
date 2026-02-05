package com.example.fblaapp.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * Room database for FBLA Connect app.
 */
@Database(
    entities = {UserEntity.class, EventEntity.class, ReminderEntity.class, AnnouncementEntity.class}, 
    version = 5, 
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "fbla_connect_db";
    private static volatile AppDatabase INSTANCE;

    public abstract UserDao userDao();
    public abstract EventDao eventDao();
    public abstract ReminderDao reminderDao();
    public abstract AnnouncementDao announcementDao();

    /**
     * Get singleton instance of the database.
     */
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DATABASE_NAME
                    )
                    .allowMainThreadQueries() // For simplicity; use AsyncTask/Executor in production
                    .fallbackToDestructiveMigration() // Reset DB on version change
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}
