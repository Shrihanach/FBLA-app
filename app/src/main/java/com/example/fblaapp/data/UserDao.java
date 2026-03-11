package com.example.fblaapp.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

/**
 * Data Access Object for User operations.
 */
@Dao
public interface UserDao {

    /**
     * Insert a new user. If email already exists, replace the record.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertUser(UserEntity user);

    /**
     * Find a user by email.
     */
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    UserEntity findByEmail(String email);

    /**
     * Find a user by ID.
     */
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    UserEntity findById(long id);

    /**
     * Check if any users exist in the database.
     */
    @Query("SELECT COUNT(*) FROM users")
    int getUserCount();

    /**
     * Update a user's role.
     */
    @Query("UPDATE users SET role = :role WHERE id = :userId")
    void updateUserRole(long userId, String role);

    /**
     * Delete all users (for testing purposes).
     */
    @Query("DELETE FROM users")
    void deleteAllUsers();
}
