package com.example.fblaapp;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fblaapp.data.AnnouncementEntity;
import com.example.fblaapp.data.AnnouncementRepository;
import com.example.fblaapp.data.AuthRepository;
import com.example.fblaapp.data.UserEntity;
import com.example.fblaapp.utils.AppExecutors;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private static final int INITIAL_LOAD_COUNT = 5;

    private RecyclerView recyclerAnnouncements;
    private BottomNavigationView bottomNavigation;
    private FloatingActionButton fabAddAnnouncement;
    private LinearLayout layoutEmpty;
    private TextView textEmptyHint;
    private TextView textRoleInfo;

    private AuthRepository authRepository;
    private AnnouncementRepository announcementRepository;
    private AnnouncementsAdapter adapter;
    private boolean isOfficer = false;
    private long currentUserId = -1;

    private List<AnnouncementEntity> allAnnouncements = new ArrayList<>();
    private int currentDisplayCount = INITIAL_LOAD_COUNT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        authRepository = AuthRepository.getInstance(this);
        announcementRepository = AnnouncementRepository.getInstance(this);

        // Check if logged in
        if (!authRepository.isLoggedIn()) {
            navigateToLogin();
            return;
        }

        initViews();
        setupUserRole();
        setupRecyclerView();
        setupBottomNavigation();
        loadAnnouncements();

        fabAddAnnouncement.setOnClickListener(v -> showAddAnnouncementDialog());
    }

    private void initViews() {
        recyclerAnnouncements = findViewById(R.id.recyclerAnnouncements);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        fabAddAnnouncement = findViewById(R.id.fabAddAnnouncement);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        textEmptyHint = findViewById(R.id.textEmptyHint);
        textRoleInfo = findViewById(R.id.textRoleInfo);
    }

    private void setupUserRole() {
        UserEntity currentUser = authRepository.getCurrentUser();
        if (currentUser != null) {
            isOfficer = currentUser.isOfficer();
            currentUserId = currentUser.getId();

            if (isOfficer) {
                fabAddAnnouncement.setVisibility(View.VISIBLE);
                textRoleInfo.setText("Officer - Post announcements");
            } else {
                fabAddAnnouncement.setVisibility(View.GONE);
                textRoleInfo.setText("Announcements");
            }
        }
    }

    private void setupRecyclerView() {
        adapter = new AnnouncementsAdapter();
        adapter.setOfficer(isOfficer);
        adapter.setCurrentUserId(currentUserId);
        adapter.setOnAnnouncementClickListener(new AnnouncementsAdapter.OnAnnouncementClickListener() {
            @Override
            public void onEditClick(AnnouncementEntity announcement) {
                showEditAnnouncementDialog(announcement);
            }

            @Override
            public void onDeleteClick(AnnouncementEntity announcement) {
                showDeleteConfirmation(announcement);
            }

            @Override
            public void onShowMoreClick() {
                loadMoreAnnouncements();
            }
        });

        recyclerAnnouncements.setLayoutManager(new LinearLayoutManager(this));
        recyclerAnnouncements.setAdapter(adapter);
    }

    private void loadAnnouncements() {
        currentDisplayCount = INITIAL_LOAD_COUNT;
        
        announcementRepository.getAllAnnouncementsLive().observe(this, announcements -> {
            allAnnouncements = announcements != null ? announcements : new ArrayList<>();
            updateDisplayedAnnouncements();
        });
    }

    private void updateDisplayedAnnouncements() {
        List<AnnouncementEntity> displayList;
        boolean showMore = false;

        if (allAnnouncements.size() > currentDisplayCount) {
            displayList = allAnnouncements.subList(0, currentDisplayCount);
            showMore = true;
        } else {
            displayList = new ArrayList<>(allAnnouncements);
            showMore = false;
        }

        adapter.setAnnouncements(displayList);
        adapter.setShowMoreVisible(showMore);

        // Update empty state
        if (allAnnouncements.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            recyclerAnnouncements.setVisibility(View.GONE);
            if (isOfficer) {
                textEmptyHint.setText("Tap the + button to post your first announcement");
            } else {
                textEmptyHint.setText("Check back later for announcements");
            }
        } else {
            layoutEmpty.setVisibility(View.GONE);
            recyclerAnnouncements.setVisibility(View.VISIBLE);
        }
    }

    private void loadMoreAnnouncements() {
        currentDisplayCount += INITIAL_LOAD_COUNT;
        updateDisplayedAnnouncements();
    }

    private void showAddAnnouncementDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_add_announcement);
        dialog.getWindow().setLayout(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        TextView dialogTitle = dialog.findViewById(R.id.dialogTitle);
        dialogTitle.setText("New Announcement");

        TextInputEditText editTitle = dialog.findViewById(R.id.editTitle);
        TextInputEditText editContent = dialog.findViewById(R.id.editContent);
        Button btnCancel = dialog.findViewById(R.id.btnCancel);
        Button btnPost = dialog.findViewById(R.id.btnPost);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnPost.setOnClickListener(v -> {
            String title = editTitle.getText() != null ? editTitle.getText().toString().trim() : "";
            String content = editContent.getText() != null ? editContent.getText().toString().trim() : "";

            if (title.isEmpty()) {
                editTitle.setError("Title is required");
                editTitle.requestFocus();
                return;
            }

            if (content.isEmpty()) {
                editContent.setError("Content is required");
                editContent.requestFocus();
                return;
            }

            postAnnouncement(title, content, dialog);
        });

        dialog.show();
    }

    private void showEditAnnouncementDialog(AnnouncementEntity announcement) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_add_announcement);
        dialog.getWindow().setLayout(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        TextView dialogTitle = dialog.findViewById(R.id.dialogTitle);
        dialogTitle.setText("Edit Announcement");

        TextInputEditText editTitle = dialog.findViewById(R.id.editTitle);
        TextInputEditText editContent = dialog.findViewById(R.id.editContent);
        Button btnCancel = dialog.findViewById(R.id.btnCancel);
        Button btnPost = dialog.findViewById(R.id.btnPost);

        // Pre-fill with existing data
        editTitle.setText(announcement.getTitle());
        editContent.setText(announcement.getContent());
        btnPost.setText("Save");

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnPost.setOnClickListener(v -> {
            String title = editTitle.getText() != null ? editTitle.getText().toString().trim() : "";
            String content = editContent.getText() != null ? editContent.getText().toString().trim() : "";

            if (title.isEmpty()) {
                editTitle.setError("Title is required");
                editTitle.requestFocus();
                return;
            }

            if (content.isEmpty()) {
                editContent.setError("Content is required");
                editContent.requestFocus();
                return;
            }

            updateAnnouncement(announcement.getId(), title, content, dialog);
        });

        dialog.show();
    }

    private void postAnnouncement(String title, String content, Dialog dialog) {
        AppExecutors.diskIO().execute(() -> {
            try {
                announcementRepository.createAnnouncement(title, content);
                runOnUiThread(() -> {
                    dialog.dismiss();
                    Toast.makeText(this, "Announcement posted!", Toast.LENGTH_SHORT).show();
                    // Reset to show latest
                    currentDisplayCount = INITIAL_LOAD_COUNT;
                });
            } catch (SecurityException e) {
                runOnUiThread(() -> 
                    Toast.makeText(this, "Only officers can post announcements", Toast.LENGTH_SHORT).show()
                );
            } catch (Exception e) {
                runOnUiThread(() -> 
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void updateAnnouncement(long announcementId, String title, String content, Dialog dialog) {
        AppExecutors.diskIO().execute(() -> {
            try {
                announcementRepository.updateAnnouncement(announcementId, title, content);
                runOnUiThread(() -> {
                    dialog.dismiss();
                    Toast.makeText(this, "Announcement updated!", Toast.LENGTH_SHORT).show();
                });
            } catch (SecurityException e) {
                runOnUiThread(() -> 
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            } catch (Exception e) {
                runOnUiThread(() -> 
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void showDeleteConfirmation(AnnouncementEntity announcement) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Announcement")
                .setMessage("Are you sure you want to delete this announcement?")
                .setPositiveButton("Delete", (dialog, which) -> deleteAnnouncement(announcement))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAnnouncement(AnnouncementEntity announcement) {
        AppExecutors.diskIO().execute(() -> {
            try {
                announcementRepository.deleteAnnouncement(announcement.getId());
                runOnUiThread(() -> 
                    Toast.makeText(this, "Announcement deleted", Toast.LENGTH_SHORT).show()
                );
            } catch (SecurityException e) {
                runOnUiThread(() -> 
                    Toast.makeText(this, "Only officers can delete announcements", Toast.LENGTH_SHORT).show()
                );
            } catch (Exception e) {
                runOnUiThread(() -> 
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, Login.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_home);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
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
                startActivity(new Intent(this, ProfileActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        bottomNavigation.setSelectedItemId(R.id.nav_home);
    }
}
