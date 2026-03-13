package com.example.fblaapp.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Repository for authentication operations.
 * Handles login, logout, user registration, and session management.
 */
public class AuthRepository {

    private static final String PREFS_NAME = "FBLAAuthPrefs";
    private static final String KEY_LOGGED_IN_USER_ID = "logged_in_user_id";

    private final UserDao userDao;
    private final SharedPreferences prefs;
    private final Context context;

    private static volatile AuthRepository INSTANCE;

    private AuthRepository(Context context) {
        this.context = context.getApplicationContext();
        this.userDao = AppDatabase.getInstance(context).userDao();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        // Seed demo users on first launch
        seedDemoUsersIfNeeded();
    }

    /**
     * Get singleton instance of AuthRepository.
     */
    public static AuthRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AuthRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new AuthRepository(context);
                }
            }
        }
        // Always ensure demo accounts are seeded with correct roles
        INSTANCE.seedDemoUsersIfNeeded();
        return INSTANCE;
    }

    /**
     * Seed demo users on first launch or after database reset.
     * Always checks if users exist and ensures their roles are correct.
     */
    private void seedDemoUsersIfNeeded() {
        // --- Demo Member ---
        UserEntity existingMember = userDao.findByEmail("member@fbla.org");
        if (existingMember == null) {
            UserEntity member = new UserEntity(
                    "Demo Member",
                    "member@fbla.org",
                    hashPassword("member123"),
                    UserEntity.ROLE_MEMBER
            );
            userDao.insertUser(member);
        } else if (!UserEntity.ROLE_MEMBER.equals(existingMember.getRole())) {
            // Fix role if it was changed
            userDao.updateUserRole(existingMember.getId(), UserEntity.ROLE_MEMBER);
        }

        // --- Demo Officer ---
        UserEntity existingOfficer = userDao.findByEmail("officer@fbla.org");
        if (existingOfficer == null) {
            UserEntity officer = new UserEntity(
                    "Demo Officer",
                    "officer@fbla.org",
                    hashPassword("officer123"),
                    UserEntity.ROLE_OFFICER
            );
            userDao.insertUser(officer);
        } else if (!UserEntity.ROLE_OFFICER.equals(existingOfficer.getRole())) {
            // Fix role if it was changed
            userDao.updateUserRole(existingOfficer.getId(), UserEntity.ROLE_OFFICER);
        }

        // --- Demo Teacher ---
        UserEntity existingTeacher = userDao.findByEmail("teacher@fbla.org");
        if (existingTeacher == null) {
            UserEntity teacher = new UserEntity(
                    "Demo Teacher",
                    "teacher@fbla.org",
                    hashPassword("teacher123"),
                    UserEntity.ROLE_TEACHER
            );
            userDao.insertUser(teacher);
        } else if (!UserEntity.ROLE_TEACHER.equals(existingTeacher.getRole())) {
            // Fix role if it was changed
            userDao.updateUserRole(existingTeacher.getId(), UserEntity.ROLE_TEACHER);
        }
    }

    /**
     * Attempt to log in with email and password.
     * @return UserEntity if successful, null otherwise.
     */
    public UserEntity login(String email, String password) {
        if (email == null || password == null) {
            return null;
        }

        UserEntity user = userDao.findByEmail(email.toLowerCase().trim());
        if (user == null) {
            return null;
        }

        // Verify password
        String hashedPassword = hashPassword(password);
        if (hashedPassword != null && hashedPassword.equals(user.getPasswordHash())) {
            // Save logged in user ID
            prefs.edit().putLong(KEY_LOGGED_IN_USER_ID, user.getId()).apply();
            return user;
        }

        return null;
    }

    /**
     * Register a new user.
     * @return UserEntity if successful, null if email already exists.
     */
    public UserEntity register(String name, String email, String password, String role) {
        if (email == null || password == null) {
            return null;
        }

        String normalizedEmail = email.toLowerCase().trim();
        
        // Check if email already exists
        UserEntity existing = userDao.findByEmail(normalizedEmail);
        if (existing != null) {
            return null; // Email already registered
        }

        // Create new user
        UserEntity newUser = new UserEntity(
                name != null ? name.trim() : "",
                normalizedEmail,
                hashPassword(password),
                role != null ? role : UserEntity.ROLE_MEMBER
        );

        long id = userDao.insertUser(newUser);
        newUser.setId(id);

        // Auto-login after registration
        prefs.edit().putLong(KEY_LOGGED_IN_USER_ID, id).apply();

        return newUser;
    }

    /**
     * Get the currently logged in user.
     * @return UserEntity if logged in, null otherwise.
     */
    public UserEntity getCurrentUser() {
        long userId = prefs.getLong(KEY_LOGGED_IN_USER_ID, -1);
        if (userId == -1) {
            return null;
        }
        return userDao.findById(userId);
    }

    /**
     * Check if a user is currently logged in.
     */
    public boolean isLoggedIn() {
        return getCurrentUser() != null;
    }

    /**
     * Log out the current user.
     */
    public void logout() {
        prefs.edit().remove(KEY_LOGGED_IN_USER_ID).apply();
    }

    /**
     * Update the role of the currently logged-in user.
     * @param role One of UserEntity.ROLE_MEMBER, ROLE_OFFICER, ROLE_PRESIDENT, ROLE_TEACHER
     */
    public void updateCurrentUserRole(String role) {
        UserEntity current = getCurrentUser();
        if (current != null && role != null) {
            userDao.updateUserRole(current.getId(), role);
        }
    }

    /**
     * Hash a password using SHA-256.
     */
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }
}
