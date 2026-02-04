package com.example.fblaapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private RecyclerView recyclerAnnouncements;
    private BottomNavigationView bottomNavigation;
    private List<Announcement> announcements;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        initViews();
        loadAnnouncements();
        setupRecyclerView();
        setupBottomNavigation();
    }

    private void initViews() {
        recyclerAnnouncements = findViewById(R.id.recyclerAnnouncements);
        bottomNavigation = findViewById(R.id.bottomNavigation);
    }

    private void loadAnnouncements() {
        announcements = new ArrayList<>();
        announcements.add(new Announcement(
                getString(R.string.announcement_1_title),
                getString(R.string.announcement_1_desc)
        ));
        announcements.add(new Announcement(
                getString(R.string.announcement_2_title),
                getString(R.string.announcement_2_desc)
        ));
        announcements.add(new Announcement(
                getString(R.string.announcement_3_title),
                getString(R.string.announcement_3_desc)
        ));
        announcements.add(new Announcement(
                getString(R.string.announcement_4_title),
                getString(R.string.announcement_4_desc)
        ));
    }

    private void setupRecyclerView() {
        recyclerAnnouncements.setLayoutManager(new LinearLayoutManager(this));
        recyclerAnnouncements.setAdapter(new AnnouncementAdapter(announcements));
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

    // Announcement data class
    static class Announcement {
        String title;
        String description;

        Announcement(String title, String description) {
            this.title = title;
            this.description = description;
        }
    }

    // Announcement Adapter
    class AnnouncementAdapter extends RecyclerView.Adapter<AnnouncementAdapter.ViewHolder> {
        private final List<Announcement> items;

        AnnouncementAdapter(List<Announcement> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_announcement, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Announcement announcement = items.get(position);
            holder.textTitle.setText(announcement.title);
            holder.textDescription.setText(announcement.description);

            holder.btnShare.setOnClickListener(v -> shareAnnouncement(announcement));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textTitle, textDescription;
            Button btnShare;

            ViewHolder(View itemView) {
                super(itemView);
                textTitle = itemView.findViewById(R.id.textTitle);
                textDescription = itemView.findViewById(R.id.textDescription);
                btnShare = itemView.findViewById(R.id.btnShare);
            }
        }
    }

    private void shareAnnouncement(Announcement announcement) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "FBLA Connect: " + announcement.title);
        shareIntent.putExtra(Intent.EXTRA_TEXT, 
                announcement.title + "\n\n" + announcement.description + "\n\n- Shared from FBLA Connect");
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share)));
    }
}
