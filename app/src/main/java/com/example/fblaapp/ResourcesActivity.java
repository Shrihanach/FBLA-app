package com.example.fblaapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ResourcesActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "FBLAConnectPrefs";
    private static final String KEY_BOOKMARKS = "bookmarks";

    private Button btnAllResources, btnBookmarks;
    private RecyclerView recyclerResources;
    private BottomNavigationView bottomNavigation;
    private SharedPreferences prefs;
    private List<Resource> allResources;
    private Set<String> bookmarkedUrls;
    private ResourceAdapter adapter;
    private boolean showingBookmarks = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resources);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        initViews();
        initResources();
        loadBookmarks();
        setupRecyclerView();
        setupListeners();
        setupBottomNavigation();
    }

    private void initViews() {
        btnAllResources = findViewById(R.id.btnAllResources);
        btnBookmarks = findViewById(R.id.btnBookmarks);
        recyclerResources = findViewById(R.id.recyclerResources);
        bottomNavigation = findViewById(R.id.bottomNavigation);
    }

    private void initResources() {
        allResources = new ArrayList<>();
        allResources.add(new Resource("FBLA Official Website", "https://www.fbla.org"));
        allResources.add(new Resource("Competitive Events", "https://www.fbla.org/divisions/fbla/fbla-competitive-events/"));
        allResources.add(new Resource("FBLA National Conference", "https://www.fbla.org/conferences/nlc/"));
        allResources.add(new Resource("FBLA Awards Program", "https://www.fbla.org/divisions/fbla/fbla-awards-program/"));
        allResources.add(new Resource("FBLA Community Service", "https://www.fbla.org/divisions/fbla/fbla-community-service/"));
        allResources.add(new Resource("March of Dimes Partnership", "https://www.fbla.org/divisions/fbla/march-of-dimes/"));
        allResources.add(new Resource("FBLA Membership Benefits", "https://www.fbla.org/membership/benefits/"));
        allResources.add(new Resource("Adviser Resources", "https://www.fbla.org/membership/advisers/"));
        allResources.add(new Resource("FBLA Publications", "https://www.fbla.org/media/publications/"));
        allResources.add(new Resource("State Chapters Directory", "https://www.fbla.org/about/state-chapters/"));
    }

    private void loadBookmarks() {
        bookmarkedUrls = new HashSet<>();
        String bookmarksJson = prefs.getString(KEY_BOOKMARKS, null);
        if (bookmarksJson != null) {
            try {
                JSONArray jsonArray = new JSONArray(bookmarksJson);
                for (int i = 0; i < jsonArray.length(); i++) {
                    bookmarkedUrls.add(jsonArray.getString(i));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveBookmarks() {
        JSONArray jsonArray = new JSONArray();
        for (String url : bookmarkedUrls) {
            jsonArray.put(url);
        }
        prefs.edit().putString(KEY_BOOKMARKS, jsonArray.toString()).apply();
    }

    private void setupRecyclerView() {
        recyclerResources.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ResourceAdapter(allResources);
        recyclerResources.setAdapter(adapter);
    }

    private void setupListeners() {
        btnAllResources.setOnClickListener(v -> showAllResources());
        btnBookmarks.setOnClickListener(v -> showBookmarkedResources());
    }

    private void showAllResources() {
        showingBookmarks = false;
        updateTabButtons();
        adapter = new ResourceAdapter(allResources);
        recyclerResources.setAdapter(adapter);
    }

    private void showBookmarkedResources() {
        showingBookmarks = true;
        updateTabButtons();
        
        List<Resource> bookmarked = new ArrayList<>();
        for (Resource resource : allResources) {
            if (bookmarkedUrls.contains(resource.url)) {
                bookmarked.add(resource);
            }
        }
        
        adapter = new ResourceAdapter(bookmarked);
        recyclerResources.setAdapter(adapter);
    }

    private void updateTabButtons() {
        if (showingBookmarks) {
            btnBookmarks.setBackgroundTintList(getColorStateList(R.color.fbla_blue));
            btnBookmarks.setTextColor(getColor(R.color.white));
            btnAllResources.setBackgroundTintList(null);
            btnAllResources.setTextColor(getColor(R.color.fbla_blue));
        } else {
            btnAllResources.setBackgroundTintList(getColorStateList(R.color.fbla_blue));
            btnAllResources.setTextColor(getColor(R.color.white));
            btnBookmarks.setBackgroundTintList(null);
            btnBookmarks.setTextColor(getColor(R.color.fbla_blue));
        }
    }

    private void toggleBookmark(Resource resource) {
        if (bookmarkedUrls.contains(resource.url)) {
            bookmarkedUrls.remove(resource.url);
            Toast.makeText(this, R.string.removed_bookmark, Toast.LENGTH_SHORT).show();
        } else {
            bookmarkedUrls.add(resource.url);
            Toast.makeText(this, R.string.bookmarked, Toast.LENGTH_SHORT).show();
        }
        saveBookmarks();
        adapter.notifyDataSetChanged();
        
        // If showing bookmarks, refresh the list
        if (showingBookmarks) {
            showBookmarkedResources();
        }
    }

    private void openUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }

    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_resources);
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

    // Resource data class
    static class Resource {
        String title;
        String url;

        Resource(String title, String url) {
            this.title = title;
            this.url = url;
        }
    }

    // Resource Adapter
    class ResourceAdapter extends RecyclerView.Adapter<ResourceAdapter.ViewHolder> {
        private final List<Resource> items;

        ResourceAdapter(List<Resource> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_resource, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Resource resource = items.get(position);
            holder.textResourceTitle.setText(resource.title);
            holder.textResourceUrl.setText(resource.url);

            // Update bookmark icon
            boolean isBookmarked = bookmarkedUrls.contains(resource.url);
            holder.btnBookmark.setImageResource(
                    isBookmarked ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off
            );

            holder.btnBookmark.setOnClickListener(v -> toggleBookmark(resource));
            holder.btnOpen.setOnClickListener(v -> openUrl(resource.url));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textResourceTitle, textResourceUrl;
            ImageButton btnBookmark;
            Button btnOpen;

            ViewHolder(View itemView) {
                super(itemView);
                textResourceTitle = itemView.findViewById(R.id.textResourceTitle);
                textResourceUrl = itemView.findViewById(R.id.textResourceUrl);
                btnBookmark = itemView.findViewById(R.id.btnBookmark);
                btnOpen = itemView.findViewById(R.id.btnOpen);
            }
        }
    }
}
