package com.example.fblaapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * MainActivity - Entry point after login
 * Redirects to HomeActivity for the main app experience
 */
public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();

        // Check if user is logged in
        if (user == null) {
            // Not logged in, go to Login
            startActivity(new Intent(this, Login.class));
        } else {
            // Logged in, go to Home
            startActivity(new Intent(this, HomeActivity.class));
        }
        
        finish();
    }
}
