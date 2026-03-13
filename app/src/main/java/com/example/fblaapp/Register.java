package com.example.fblaapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.fblaapp.data.AuthRepository;
import com.example.fblaapp.data.UserEntity;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
import org.json.JSONObject;

public class Register extends AppCompatActivity {

    private TextInputEditText editTextName, editTextGrade, editTextEmail, editTextPassword;
    private Button buttonRegister;
    private ProgressBar progressBar;
    private TextView textViewLogin;
    private AuthRepository authRepository;

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is already logged in
        if (authRepository != null && authRepository.isLoggedIn()) {
            navigateToHome();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        // Initialize AuthRepository
        authRepository = AuthRepository.getInstance(this);

        // Check if already logged in
        if (authRepository.isLoggedIn()) {
            navigateToHome();
            return;
        }

        initViews();
        setupListeners();
        setupWindowInsets();
    }

    private void initViews() {
        editTextName = findViewById(R.id.name);
        editTextGrade = findViewById(R.id.grade);
        editTextEmail = findViewById(R.id.email);
        editTextPassword = findViewById(R.id.password);
        buttonRegister = findViewById(R.id.btn_register);
        progressBar = findViewById(R.id.progressBar);
        textViewLogin = findViewById(R.id.loginNow);
    }

    private void setupListeners() {
        // Login link click
        textViewLogin.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), Login.class);
            startActivity(intent);
            finish();
        });

        // Register button click
        buttonRegister.setOnClickListener(v -> attemptRegister());
    }

    private void attemptRegister() {
        String name = editTextName.getText() != null ?
                editTextName.getText().toString().trim() : "";
        String grade = editTextGrade.getText() != null ?
                editTextGrade.getText().toString().trim() : "";
        String email = editTextEmail.getText() != null ? 
                editTextEmail.getText().toString().trim() : "";
        String password = editTextPassword.getText() != null ? 
                editTextPassword.getText().toString() : "";

        // Validate name
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, "Enter your name", Toast.LENGTH_SHORT).show();
            editTextName.requestFocus();
            return;
        }

        // Validate grade
        if (TextUtils.isEmpty(grade)) {
            Toast.makeText(this, "Enter your grade", Toast.LENGTH_SHORT).show();
            editTextGrade.requestFocus();
            return;
        }

        int gradeNum;
        try {
            gradeNum = Integer.parseInt(grade);
            if (gradeNum < 1 || gradeNum > 12) {
                Toast.makeText(this, "Grade must be between 1 and 12", Toast.LENGTH_SHORT).show();
                editTextGrade.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Enter a valid grade number", Toast.LENGTH_SHORT).show();
            editTextGrade.requestFocus();
            return;
        }

        // Validate email
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Enter email", Toast.LENGTH_SHORT).show();
            editTextEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Enter a valid email address", Toast.LENGTH_SHORT).show();
            editTextEmail.requestFocus();
            return;
        }

        // Validate password
        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Enter password", Toast.LENGTH_SHORT).show();
            editTextPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            editTextPassword.requestFocus();
            return;
        }

        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        buttonRegister.setEnabled(false);

        // Attempt registration (default role is MEMBER)
        UserEntity user = authRepository.register(name, email, password, UserEntity.ROLE_MEMBER);

        progressBar.setVisibility(View.GONE);
        buttonRegister.setEnabled(true);

        if (user != null) {
            // Save grade to profile SharedPreferences
            saveProfileData(name, grade);

            Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show();
            navigateToHome();
        } else {
            // Registration failed (email already exists)
            Toast.makeText(this, "Email already registered", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveProfileData(String name, String grade) {
        SharedPreferences prefs = getSharedPreferences("FBLAConnectPrefs", MODE_PRIVATE);
        try {
            JSONObject profile = new JSONObject();
            profile.put("name", name);
            profile.put("grade", grade);
            profile.put("role", "Member");
            prefs.edit().putString("profile", profile.toString()).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void navigateToHome() {
        Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}
