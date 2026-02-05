package com.example.fblaapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fblaapp.data.AuthRepository;
import com.example.fblaapp.data.UserEntity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONException;
import org.json.JSONObject;

public class ProfileActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "FBLAConnectPrefs";
    private static final String KEY_PROFILE = "profile";

    private EditText editName, editChapter;
    private Spinner spinnerRole;
    private Button btnSave, btnLogout;
    private TextView textUserEmail, textUserRole;
    private BottomNavigationView bottomNavigation;
    private SharedPreferences prefs;
    private AuthRepository authRepository;

    private String[] roles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        authRepository = AuthRepository.getInstance(this);

        // Check if logged in
        if (!authRepository.isLoggedIn()) {
            navigateToLogin();
            return;
        }

        initViews();
        displayUserInfo();
        setupRoleSpinner();
        loadProfile();
        setupListeners();
        setupBottomNavigation();
    }

    private void initViews() {
        editName = findViewById(R.id.editName);
        editChapter = findViewById(R.id.editChapter);
        spinnerRole = findViewById(R.id.spinnerRole);
        btnSave = findViewById(R.id.btnSave);
        btnLogout = findViewById(R.id.btnLogout);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        
        // These might not exist in the layout, so check first
        textUserEmail = findViewById(R.id.textUserEmail);
        textUserRole = findViewById(R.id.textUserRole);
    }

    private void displayUserInfo() {
        UserEntity currentUser = authRepository.getCurrentUser();
        if (currentUser != null) {
            // Pre-fill name from user account if profile name is empty
            String profileName = getProfileName();
            if (profileName == null || profileName.isEmpty()) {
                editName.setText(currentUser.getName());
            }

            // Display user email and role if views exist
            if (textUserEmail != null) {
                textUserEmail.setText(currentUser.getEmail());
            }
            if (textUserRole != null) {
                String roleDisplay = currentUser.isOfficer() ? "Officer" : "Member";
                textUserRole.setText(roleDisplay);
            }
        }
    }

    private String getProfileName() {
        String profileJson = prefs.getString(KEY_PROFILE, null);
        if (profileJson != null) {
            try {
                JSONObject profile = new JSONObject(profileJson);
                return profile.optString("name", "");
            } catch (JSONException e) {
                return "";
            }
        }
        return "";
    }

    private void setupRoleSpinner() {
        roles = new String[]{
                getString(R.string.role_member),
                getString(R.string.role_president),
                getString(R.string.role_vice_president),
                getString(R.string.role_secretary),
                getString(R.string.role_treasurer),
                getString(R.string.role_reporter),
                getString(R.string.role_historian),
                getString(R.string.role_parliamentarian),
                getString(R.string.role_advisor)
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                roles
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(adapter);
    }

    private void loadProfile() {
        String profileJson = prefs.getString(KEY_PROFILE, null);
        if (profileJson != null) {
            try {
                JSONObject profile = new JSONObject(profileJson);
                editName.setText(profile.optString("name", ""));
                editChapter.setText(profile.optString("chapter", ""));

                String savedRole = profile.optString("role", "");
                for (int i = 0; i < roles.length; i++) {
                    if (roles[i].equals(savedRole)) {
                        spinnerRole.setSelection(i);
                        break;
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void setupListeners() {
        btnSave.setOnClickListener(v -> saveProfile());
        btnLogout.setOnClickListener(v -> logout());
    }

    private void saveProfile() {
        String name = editName.getText().toString().trim();
        String chapter = editChapter.getText().toString().trim();
        String role = spinnerRole.getSelectedItem().toString();

        try {
            JSONObject profile = new JSONObject();
            profile.put("name", name);
            profile.put("chapter", chapter);
            profile.put("role", role);

            prefs.edit().putString(KEY_PROFILE, profile.toString()).apply();
            Toast.makeText(this, R.string.profile_saved, Toast.LENGTH_SHORT).show();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void logout() {
        // Logout from AuthRepository
        authRepository.logout();
        navigateToLogin();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, Login.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_profile);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_calendar) {
                startActivity(new Intent(this, CalendarActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_resources) {
                startActivity(new Intent(this, ResourcesActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_social) {
                startActivity(new Intent(this, SocialActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_profile) {
                return true;
            }
            return false;
        });
    }
}
