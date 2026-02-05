package com.example.fblaapp.data;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * User entity for Room database.
 * Represents a user in the FBLA Connect app.
 */
@Entity(tableName = "users", indices = {@Index(value = "email", unique = true)})
public class UserEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String name;
    private String email;
    private String passwordHash;
    private String role; // "MEMBER" or "OFFICER"

    // Constructor
    public UserEntity(String name, String email, String passwordHash, String role) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    // Role constants
    public static final String ROLE_MEMBER = "MEMBER";
    public static final String ROLE_OFFICER = "OFFICER";

    public boolean isOfficer() {
        return ROLE_OFFICER.equals(role);
    }

    public boolean isMember() {
        return ROLE_MEMBER.equals(role);
    }
}
