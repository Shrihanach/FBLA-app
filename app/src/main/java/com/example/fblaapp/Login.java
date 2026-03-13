package com.example.fblaapp;

import android.content.Intent;
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

public class Login extends AppCompatActivity {

    private TextInputEditText editTextEmail, editTextPassword;
    private Button buttonLogin;
    private Button btnDemoOfficer, btnDemoMember, btnDemoTeacher;
    private ProgressBar progressBar;
    private TextView textViewRegister;
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
        setContentView(R.layout.activity_login);

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
        editTextEmail = findViewById(R.id.email);
        editTextPassword = findViewById(R.id.password);
        buttonLogin = findViewById(R.id.btn_login);
        progressBar = findViewById(R.id.progressBar);
        textViewRegister = findViewById(R.id.registerNow);
        btnDemoOfficer = findViewById(R.id.btnDemoOfficer);
        btnDemoMember = findViewById(R.id.btnDemoMember);
        btnDemoTeacher = findViewById(R.id.btnDemoTeacher);
    }

    private void setupListeners() {
        // Register link click
        textViewRegister.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), Register.class);
            startActivity(intent);
            finish();
        });

        // Login button click
        buttonLogin.setOnClickListener(v -> attemptLogin());

        // Demo account quick-login buttons
        btnDemoOfficer.setOnClickListener(v -> loginWithDemo("officer@fbla.org", "officer123"));
        btnDemoMember.setOnClickListener(v -> loginWithDemo("member@fbla.org", "member123"));
        btnDemoTeacher.setOnClickListener(v -> loginWithDemo("teacher@fbla.org", "teacher123"));
    }

    private void loginWithDemo(String email, String password) {
        progressBar.setVisibility(View.VISIBLE);

        UserEntity user = authRepository.login(email, password);

        progressBar.setVisibility(View.GONE);

        if (user != null) {
            Toast.makeText(this, "Welcome, " + user.getName() + " (" + user.getRoleDisplayName() + ")",
                    Toast.LENGTH_SHORT).show();
            navigateToHome();
        } else {
            Toast.makeText(this, "Demo account error. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void attemptLogin() {
        String email = editTextEmail.getText() != null ? 
                editTextEmail.getText().toString().trim() : "";
        String password = editTextPassword.getText() != null ? 
                editTextPassword.getText().toString() : "";

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

        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        buttonLogin.setEnabled(false);

        // Attempt login
        UserEntity user = authRepository.login(email, password);

        progressBar.setVisibility(View.GONE);
        buttonLogin.setEnabled(true);

        if (user != null) {
            // Login successful
            Toast.makeText(this, "Login Successful! Welcome, " + user.getName(), 
                    Toast.LENGTH_SHORT).show();
            navigateToHome();
        } else {
            // Login failed
            Toast.makeText(this, "Invalid login", Toast.LENGTH_SHORT).show();
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
