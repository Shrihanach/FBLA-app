package com.example.fblaapp;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.fblaapp.data.AuthRepository;
import com.example.fblaapp.data.UserEntity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "FBLAConnectPrefs";
    private static final String KEY_PROFILE = "profile";
    private static final String KEY_PROFILE_IMAGE = "profile_image_path";
    private static final String PROFILE_IMAGE_FILENAME = "profile_picture.jpg";
    private static final int REQUEST_PERMISSION_CODE = 100;

    private EditText editName, editChapter;
    private Spinner spinnerRole;
    private Button btnSave, btnLogout;
    private TextView textUserEmail, textUserRole;
    private BottomNavigationView bottomNavigation;
    private CircleImageView imageProfilePicture;
    private FrameLayout profilePictureContainer;
    private SharedPreferences prefs;
    private AuthRepository authRepository;

    private String[] roles;
    private Uri cameraImageUri;
    
    // Activity result launchers
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;

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

        // Initialize activity result launchers
        initActivityResultLaunchers();
        
        initViews();
        displayUserInfo();
        setupRoleSpinner();
        loadProfile();
        loadProfilePicture();
        setupListeners();
        setupBottomNavigation();
    }
    
    private void initActivityResultLaunchers() {
        // Gallery picker launcher
        galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        saveProfilePicture(selectedImageUri);
                    }
                }
            }
        );
        
        // Camera launcher
        cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    if (cameraImageUri != null) {
                        saveProfilePicture(cameraImageUri);
                    }
                }
            }
        );
    }

    private void initViews() {
        editName = findViewById(R.id.editName);
        editChapter = findViewById(R.id.editChapter);
        spinnerRole = findViewById(R.id.spinnerRole);
        btnSave = findViewById(R.id.btnSave);
        btnLogout = findViewById(R.id.btnLogout);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        imageProfilePicture = findViewById(R.id.imageProfilePicture);
        profilePictureContainer = findViewById(R.id.profilePictureContainer);
        
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
        profilePictureContainer.setOnClickListener(v -> showImagePickerDialog());
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
    
    // ==================== Profile Picture Methods ====================
    
    private void showImagePickerDialog() {
        String[] options = {"Take Photo", "Choose from Gallery", "Remove Photo"};
        
        new AlertDialog.Builder(this)
            .setTitle("Profile Picture")
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0: // Take Photo
                        checkCameraPermissionAndOpen();
                        break;
                    case 1: // Choose from Gallery
                        openGallery();
                        break;
                    case 2: // Remove Photo
                        removeProfilePicture();
                        break;
                }
            })
            .show();
    }
    
    private void checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.CAMERA}, REQUEST_PERMISSION_CODE);
        } else {
            openCamera();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission is required to take photos", 
                    Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = createImageFile();
            if (photoFile != null) {
                cameraImageUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", photoFile);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
                cameraLauncher.launch(cameraIntent);
            }
        } else {
            Toast.makeText(this, "No camera app available", Toast.LENGTH_SHORT).show();
        }
    }
    
    private File createImageFile() {
        try {
            File storageDir = getFilesDir();
            return new File(storageDir, "camera_temp.jpg");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, 
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryIntent.setType("image/*");
        galleryLauncher.launch(galleryIntent);
    }
    
    private void saveProfilePicture(Uri imageUri) {
        try {
            // Read the image from URI
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                return;
            }
            
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();
            
            if (bitmap == null) {
                Toast.makeText(this, "Failed to decode image", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Resize bitmap to reasonable size (500x500 max)
            bitmap = resizeBitmap(bitmap, 500);
            
            // Save to internal storage
            File profileImageFile = new File(getFilesDir(), PROFILE_IMAGE_FILENAME);
            FileOutputStream fos = new FileOutputStream(profileImageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();
            
            // Save path to SharedPreferences
            prefs.edit().putString(KEY_PROFILE_IMAGE, profileImageFile.getAbsolutePath()).apply();
            
            // Update the ImageView
            imageProfilePicture.setImageBitmap(bitmap);
            
            Toast.makeText(this, "Profile picture updated", Toast.LENGTH_SHORT).show();
            
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save profile picture", Toast.LENGTH_SHORT).show();
        }
    }
    
    private Bitmap resizeBitmap(Bitmap original, int maxSize) {
        int width = original.getWidth();
        int height = original.getHeight();
        
        float ratio = Math.min((float) maxSize / width, (float) maxSize / height);
        
        if (ratio >= 1.0f) {
            return original; // No need to resize
        }
        
        int newWidth = Math.round(width * ratio);
        int newHeight = Math.round(height * ratio);
        
        return Bitmap.createScaledBitmap(original, newWidth, newHeight, true);
    }
    
    private void loadProfilePicture() {
        String imagePath = prefs.getString(KEY_PROFILE_IMAGE, null);
        if (imagePath != null) {
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
                if (bitmap != null) {
                    imageProfilePicture.setImageBitmap(bitmap);
                }
            }
        }
    }
    
    private void removeProfilePicture() {
        // Delete the file
        String imagePath = prefs.getString(KEY_PROFILE_IMAGE, null);
        if (imagePath != null) {
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                imageFile.delete();
            }
        }
        
        // Clear from preferences
        prefs.edit().remove(KEY_PROFILE_IMAGE).apply();
        
        // Reset to placeholder
        imageProfilePicture.setImageResource(R.drawable.profile_picture_placeholder);
        
        Toast.makeText(this, "Profile picture removed", Toast.LENGTH_SHORT).show();
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
