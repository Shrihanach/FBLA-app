package com.example.fblaapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class SocialActivity extends AppCompatActivity {

    // Social media URLs
    private static final String URL_FACEBOOK = "https://www.facebook.com/FutureBusinessLeaders";
    private static final String URL_TWITTER = "https://twitter.com/ABORTI";
    private static final String URL_INSTAGRAM = "https://www.instagram.com/fbla_pbl/";
    private static final String URL_LINKEDIN = "https://www.linkedin.com/company/fbla-pbl/";
    private static final String URL_YOUTUBE = "https://www.youtube.com/user/ABORTI";
    private static final String URL_WEBSITE = "https://www.fbla.org";

    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_social);

        initViews();
        setupClickListeners();
        setupBottomNavigation();
    }

    private void initViews() {
        bottomNavigation = findViewById(R.id.bottomNavigation);
    }

    private void setupClickListeners() {
        findViewById(R.id.btnFacebook).setOnClickListener(v -> openUrl(URL_FACEBOOK));
        findViewById(R.id.btnTwitter).setOnClickListener(v -> openUrl(URL_TWITTER));
        findViewById(R.id.btnInstagram).setOnClickListener(v -> openUrl(URL_INSTAGRAM));
        findViewById(R.id.btnLinkedIn).setOnClickListener(v -> openUrl(URL_LINKEDIN));
        findViewById(R.id.btnYouTube).setOnClickListener(v -> openUrl(URL_YOUTUBE));
        findViewById(R.id.btnWebsite).setOnClickListener(v -> openUrl(URL_WEBSITE));
    }

    private void openUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }

    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_social);
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
}
