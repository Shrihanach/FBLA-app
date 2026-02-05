package com.example.fblaapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fblaapp.data.AuthRepository;

/**
 * MainActivity - Entry point after login
 * Redirects to HomeActivity or Login based on auth state.
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AuthRepository authRepository = AuthRepository.getInstance(this);

        // Check if user is logged in
        if (authRepository.isLoggedIn()) {
            // Logged in, go to Home
            startActivity(new Intent(this, HomeActivity.class));
        } else {
            // Not logged in, go to Login
            startActivity(new Intent(this, Login.class));
        }

        finish();
    }
}
