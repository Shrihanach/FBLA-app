package com.example.fblaapp;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fblaapp.data.AuthRepository;
import com.example.fblaapp.data.FirestoreResource;
import com.example.fblaapp.data.UserEntity;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Fragment for displaying and managing FBLA chapter resources.
 * Shows 3 category cards. Objective Testing shows a 2-column event grid.
 * Presentation Events and BAA show Firestore resource lists.
 */
public class ResourcesFragment extends Fragment {

    private static final String COLLECTION_RESOURCES = "resources";
    private static final String STORAGE_PATH = "resources/";

    // Views - Category cards
    private ScrollView layoutCategoryCards;
    private MaterialCardView cardObjectiveTesting;
    private MaterialCardView cardPresentationEvents;
    private MaterialCardView cardBAA;

    // Views - Objective testing grid
    private LinearLayout layoutObjectiveGrid;
    private ImageButton btnBackGrid;
    private RecyclerView recyclerEventTiles;

    // Views - Resource list
    private LinearLayout layoutResourceList;
    private ImageButton btnBack;
    private TextView textCurrentCategory;
    private RecyclerView recyclerResources;
    private ProgressBar progressLoading;
    private LinearLayout layoutEmpty;
    private LinearLayout layoutError;
    private TextView textEmptyHint;
    private TextView textErrorMessage;
    private Button btnRetry;
    private FloatingActionButton fabUpload;
    private TextView badgeOfficer;

    // Data
    private FirebaseFirestore firestore;
    private FirebaseStorage storage;
    private AuthRepository authRepository;
    private ResourcesAdapter adapter;
    private EventTileAdapter eventTileAdapter;
    private final List<FirestoreResource> allResources = new ArrayList<>();
    private String currentCategory = null;
    private boolean isOfficer = false;
    private String currentUserName = "";

    // File picker
    private Uri selectedFileUri = null;
    private String selectedFileName = null;
    private ActivityResultLauncher<Intent> filePickerLauncher;
    private Dialog currentUploadDialog;

    // FBLA Objective Test Events
    private static final String[] OBJECTIVE_TEST_EVENTS = {
            "Accounting I",
            "Accounting II",
            "Agribusiness",
            "Business Calculations",
            "Business Communication",
            "Business Law",
            "Computer Problem Solving",
            "Cyber Security",
            "Database Design & Applications",
            "Economics",
            "Health Care Administration",
            "Insurance & Risk Management",
            "Introduction to Business",
            "Introduction to Business Communication",
            "Introduction to Financial Math",
            "Introduction to Information Technology",
            "Introduction to Parliamentary Procedure",
            "Journalism",
            "Marketing",
            "Networking Infrastructure",
            "Organizational Leadership",
            "Personal Finance",
            "Securities & Investments",
            "Supply Chain Management",
            "UX Design"
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase safely
        try {
            firestore = FirebaseFirestore.getInstance();
            storage = FirebaseStorage.getInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        authRepository = AuthRepository.getInstance(requireContext());

        // Setup file picker
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            selectedFileUri = uri;
                            selectedFileName = getFileName(uri);
                            updateFileSelectionUI();
                        }
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_resources, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupUserRole();
        setupRecyclerView();
        setupEventTileGrid();
        setupCategoryCards();
        setupClickListeners();
    }

    private void initViews(View view) {
        // Category cards view
        layoutCategoryCards = view.findViewById(R.id.layoutCategoryCards);
        cardObjectiveTesting = view.findViewById(R.id.cardObjectiveTesting);
        cardPresentationEvents = view.findViewById(R.id.cardPresentationEvents);
        cardBAA = view.findViewById(R.id.cardBAA);

        // Objective testing grid
        layoutObjectiveGrid = view.findViewById(R.id.layoutObjectiveGrid);
        btnBackGrid = view.findViewById(R.id.btnBackGrid);
        recyclerEventTiles = view.findViewById(R.id.recyclerEventTiles);

        // Resource list view
        layoutResourceList = view.findViewById(R.id.layoutResourceList);
        btnBack = view.findViewById(R.id.btnBack);
        textCurrentCategory = view.findViewById(R.id.textCurrentCategory);
        recyclerResources = view.findViewById(R.id.recyclerResources);
        progressLoading = view.findViewById(R.id.progressLoading);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        layoutError = view.findViewById(R.id.layoutError);
        textEmptyHint = view.findViewById(R.id.textEmptyHint);
        textErrorMessage = view.findViewById(R.id.textErrorMessage);
        btnRetry = view.findViewById(R.id.btnRetry);
        fabUpload = view.findViewById(R.id.fabUpload);
        badgeOfficer = view.findViewById(R.id.badgeOfficer);
    }

    private void setupUserRole() {
        UserEntity currentUser = authRepository.getCurrentUser();
        if (currentUser != null) {
            isOfficer = currentUser.isOfficer();
            currentUserName = currentUser.getName();

            if (isOfficer) {
                fabUpload.setVisibility(View.VISIBLE);
                badgeOfficer.setVisibility(View.VISIBLE);
                textEmptyHint.setText(R.string.empty_hint_officer);
            } else {
                fabUpload.setVisibility(View.GONE);
                badgeOfficer.setVisibility(View.GONE);
                textEmptyHint.setText(R.string.empty_hint_member);
            }
        }
    }

    private void setupRecyclerView() {
        adapter = new ResourcesAdapter();
        recyclerResources.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerResources.setAdapter(adapter);
    }

    private void setupEventTileGrid() {
        eventTileAdapter = new EventTileAdapter(OBJECTIVE_TEST_EVENTS);
        recyclerEventTiles.setLayoutManager(new GridLayoutManager(getContext(), 2));
        recyclerEventTiles.setAdapter(eventTileAdapter);
    }

    private void setupCategoryCards() {
        // Objective Testing → show 2-column grid
        cardObjectiveTesting.setOnClickListener(v -> openObjectiveTestingGrid());

        // Presentation Events → show Firestore resource list
        cardPresentationEvents.setOnClickListener(v -> openCategory("Presentation Event Resources"));

        // BAA → show Firestore resource list
        cardBAA.setOnClickListener(v -> openCategory("Business Achievement Awards"));
    }

    private void openObjectiveTestingGrid() {
        layoutCategoryCards.setVisibility(View.GONE);
        layoutResourceList.setVisibility(View.GONE);
        layoutObjectiveGrid.setVisibility(View.VISIBLE);
    }

    private void openCategory(String category) {
        currentCategory = category;
        textCurrentCategory.setText(category);

        // Switch to resource list view
        layoutCategoryCards.setVisibility(View.GONE);
        layoutObjectiveGrid.setVisibility(View.GONE);
        layoutResourceList.setVisibility(View.VISIBLE);

        // Load resources for this category
        loadResources();
    }

    private void showCategoryCards() {
        currentCategory = null;
        layoutCategoryCards.setVisibility(View.VISIBLE);
        layoutObjectiveGrid.setVisibility(View.GONE);
        layoutResourceList.setVisibility(View.GONE);
    }

    private void setupClickListeners() {
        btnRetry.setOnClickListener(v -> loadResources());
        fabUpload.setOnClickListener(v -> showUploadDialog());
        btnBack.setOnClickListener(v -> showCategoryCards());
        btnBackGrid.setOnClickListener(v -> showCategoryCards());
    }

    private void loadResources() {
        if (firestore == null) {
            showError("Firebase is not configured. Please check your setup.");
            return;
        }

        showLoading();

        try {
            firestore.collection(COLLECTION_RESOURCES)
                    .orderBy("uploadedAt", Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        allResources.clear();
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            FirestoreResource resource = document.toObject(FirestoreResource.class);
                            allResources.add(resource);
                        }
                        filterResources();
                    })
                    .addOnFailureListener(e -> {
                        showError(e.getMessage());
                    });
        } catch (Exception e) {
            showError("Error loading resources: " + e.getMessage());
        }
    }

    private void filterResources() {
        List<FirestoreResource> filtered = new ArrayList<>();

        if (currentCategory == null) {
            filtered.addAll(allResources);
        } else {
            for (FirestoreResource resource : allResources) {
                if (currentCategory.equalsIgnoreCase(resource.getCategory())) {
                    filtered.add(resource);
                }
            }
        }

        adapter.setResources(filtered);

        if (filtered.isEmpty()) {
            showEmpty();
        } else {
            showContent();
        }
    }

    private void showLoading() {
        progressLoading.setVisibility(View.VISIBLE);
        recyclerResources.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);
    }

    private void showContent() {
        progressLoading.setVisibility(View.GONE);
        recyclerResources.setVisibility(View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);
    }

    private void showEmpty() {
        progressLoading.setVisibility(View.GONE);
        recyclerResources.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.VISIBLE);
        layoutError.setVisibility(View.GONE);
    }

    private void showError(String message) {
        progressLoading.setVisibility(View.GONE);
        recyclerResources.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);
        layoutError.setVisibility(View.VISIBLE);
        textErrorMessage.setText(message != null ? message : "Unknown error occurred");
    }

    // ==================== Upload Dialog ====================

    private void showUploadDialog() {
        if (!isOfficer) {
            Toast.makeText(getContext(), "Only officers can upload resources", Toast.LENGTH_SHORT).show();
            return;
        }

        if (firestore == null || storage == null) {
            Toast.makeText(getContext(), "Firebase is not configured", Toast.LENGTH_SHORT).show();
            return;
        }

        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_upload_resource);
        dialog.getWindow().setLayout(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        currentUploadDialog = dialog;

        // Get views
        TextInputEditText editTitle = dialog.findViewById(R.id.editTitle);
        TextInputEditText editDescription = dialog.findViewById(R.id.editDescription);
        AutoCompleteTextView spinnerCategory = dialog.findViewById(R.id.spinnerCategory);
        Button btnSelectFile = dialog.findViewById(R.id.btnSelectFile);
        Button btnAddLink = dialog.findViewById(R.id.btnAddLink);
        LinearLayout layoutSelectedFile = dialog.findViewById(R.id.layoutSelectedFile);
        TextView textSelectedFileName = dialog.findViewById(R.id.textSelectedFileName);
        ImageButton btnClearFile = dialog.findViewById(R.id.btnClearFile);
        TextInputLayout layoutLinkInput = dialog.findViewById(R.id.layoutLinkInput);
        TextInputEditText editLink = dialog.findViewById(R.id.editLink);
        LinearLayout layoutUploadProgress = dialog.findViewById(R.id.layoutUploadProgress);
        ProgressBar progressUpload = dialog.findViewById(R.id.progressUpload);
        TextView textUploadStatus = dialog.findViewById(R.id.textUploadStatus);
        Button btnCancel = dialog.findViewById(R.id.btnCancel);
        Button btnUpload = dialog.findViewById(R.id.btnUpload);

        // Setup category dropdown
        String[] categories = {"Objective Testing Resources", "Presentation Event Resources", "Business Achievement Awards"};
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                categories
        );
        spinnerCategory.setAdapter(categoryAdapter);

        // Pre-fill category if we're inside a category view
        if (currentCategory != null) {
            spinnerCategory.setText(currentCategory, false);
        }

        // Reset file selection
        selectedFileUri = null;
        selectedFileName = null;

        // File selection button
        btnSelectFile.setOnClickListener(v -> {
            layoutLinkInput.setVisibility(View.GONE);
            openFilePicker();
        });

        // Add link button
        btnAddLink.setOnClickListener(v -> {
            layoutSelectedFile.setVisibility(View.GONE);
            layoutLinkInput.setVisibility(View.VISIBLE);
            selectedFileUri = null;
            selectedFileName = null;
        });

        // Clear file selection
        btnClearFile.setOnClickListener(v -> {
            selectedFileUri = null;
            selectedFileName = null;
            layoutSelectedFile.setVisibility(View.GONE);
        });

        // Cancel button
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // Upload button
        btnUpload.setOnClickListener(v -> {
            String title = editTitle.getText() != null ? editTitle.getText().toString().trim() : "";
            String description = editDescription.getText() != null ? editDescription.getText().toString().trim() : "";
            String category = spinnerCategory.getText() != null ? spinnerCategory.getText().toString().trim() : "";
            String link = editLink.getText() != null ? editLink.getText().toString().trim() : "";

            // Validation
            if (title.isEmpty()) {
                editTitle.setError("Title is required");
                editTitle.requestFocus();
                return;
            }

            if (description.isEmpty()) {
                editDescription.setError("Description is required");
                editDescription.requestFocus();
                return;
            }

            if (category.isEmpty()) {
                spinnerCategory.setError("Category is required");
                spinnerCategory.requestFocus();
                return;
            }

            boolean hasFile = selectedFileUri != null;
            boolean hasLink = !link.isEmpty();

            if (!hasFile && !hasLink) {
                Toast.makeText(getContext(), "Please select a file or add a link", Toast.LENGTH_SHORT).show();
                return;
            }

            // Show progress
            layoutUploadProgress.setVisibility(View.VISIBLE);
            btnUpload.setEnabled(false);
            btnCancel.setEnabled(false);

            if (hasFile) {
                uploadFileAndSaveResource(title, description, category,
                        progressUpload, textUploadStatus, dialog);
            } else {
                saveResourceToFirestore(title, description, category, link, "link", dialog);
            }
        });

        dialog.show();
    }

    private void updateFileSelectionUI() {
        if (currentUploadDialog != null && selectedFileName != null) {
            LinearLayout layoutSelectedFile = currentUploadDialog.findViewById(R.id.layoutSelectedFile);
            TextView textSelectedFileName = currentUploadDialog.findViewById(R.id.textSelectedFileName);
            TextInputLayout layoutLinkInput = currentUploadDialog.findViewById(R.id.layoutLinkInput);

            layoutSelectedFile.setVisibility(View.VISIBLE);
            textSelectedFileName.setText(selectedFileName);
            layoutLinkInput.setVisibility(View.GONE);
        }
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        filePickerLauncher.launch(intent);
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) {
                        result = cursor.getString(index);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result != null ? result.lastIndexOf('/') : -1;
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private String getFileExtension(String fileName) {
        if (fileName == null) return "";
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            return fileName.substring(lastDot + 1).toLowerCase();
        }
        return "";
    }

    private void uploadFileAndSaveResource(String title, String description, String category,
                                           ProgressBar progressUpload, TextView textUploadStatus,
                                           Dialog dialog) {
        if (selectedFileUri == null) return;

        String fileExtension = getFileExtension(selectedFileName);
        String uniqueFileName = UUID.randomUUID().toString() + "." + fileExtension;
        StorageReference storageRef = storage.getReference().child(STORAGE_PATH + uniqueFileName);

        UploadTask uploadTask = storageRef.putFile(selectedFileUri);

        uploadTask.addOnProgressListener(snapshot -> {
            double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
            progressUpload.setProgress((int) progress);
            textUploadStatus.setText(getString(R.string.uploading_progress, (int) progress));
        }).addOnSuccessListener(taskSnapshot -> {
            textUploadStatus.setText(R.string.getting_download_url);
            storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                saveResourceToFirestore(title, description, category, uri.toString(), fileExtension, dialog);
            }).addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Failed to get download URL: " + e.getMessage(), Toast.LENGTH_LONG).show();
                resetUploadDialog(dialog);
            });
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            resetUploadDialog(dialog);
        });
    }

    private void saveResourceToFirestore(String title, String description, String category,
                                         String fileUrl, String fileType, Dialog dialog) {
        Map<String, Object> resource = new HashMap<>();
        resource.put("title", title);
        resource.put("description", description);
        resource.put("category", category);
        resource.put("fileUrl", fileUrl);
        resource.put("fileType", fileType);
        resource.put("uploadedBy", currentUserName);
        resource.put("uploadedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

        firestore.collection(COLLECTION_RESOURCES)
                .add(resource)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(getContext(), "Resource uploaded successfully!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    loadResources();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to save resource: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    resetUploadDialog(dialog);
                });
    }

    private void resetUploadDialog(Dialog dialog) {
        LinearLayout layoutUploadProgress = dialog.findViewById(R.id.layoutUploadProgress);
        Button btnUpload = dialog.findViewById(R.id.btnUpload);
        Button btnCancel = dialog.findViewById(R.id.btnCancel);

        layoutUploadProgress.setVisibility(View.GONE);
        btnUpload.setEnabled(true);
        btnCancel.setEnabled(true);
    }

    // ==================== Event Tile Adapter (2-column grid) ====================

    private class EventTileAdapter extends RecyclerView.Adapter<EventTileAdapter.TileViewHolder> {

        private final String[] eventNames;

        EventTileAdapter(String[] eventNames) {
            this.eventNames = eventNames;
        }

        @NonNull
        @Override
        public TileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_event_tile, parent, false);
            return new TileViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull TileViewHolder holder, int position) {
            String eventName = eventNames[position];
            holder.textEventName.setText(eventName);

            // Placeholder icon visible by default (will be hidden when real images are set)
            holder.imgPlaceholderIcon.setVisibility(View.VISIBLE);
            holder.imgEventTile.setImageDrawable(null);

            // Click listener — can be expanded later to open event-specific resources
            holder.itemView.setOnClickListener(v -> {
                Toast.makeText(getContext(), eventName + " — resources coming soon!", Toast.LENGTH_SHORT).show();
            });
        }

        @Override
        public int getItemCount() {
            return eventNames.length;
        }

        class TileViewHolder extends RecyclerView.ViewHolder {
            TextView textEventName;
            ImageView imgEventTile;
            ImageView imgPlaceholderIcon;

            TileViewHolder(View itemView) {
                super(itemView);
                textEventName = itemView.findViewById(R.id.textEventName);
                imgEventTile = itemView.findViewById(R.id.imgEventTile);
                imgPlaceholderIcon = itemView.findViewById(R.id.imgPlaceholderIcon);
            }
        }
    }

    // ==================== Resources Adapter (Firestore list) ====================

    private class ResourcesAdapter extends RecyclerView.Adapter<ResourcesAdapter.ViewHolder> {

        private List<FirestoreResource> resources = new ArrayList<>();

        public void setResources(List<FirestoreResource> resources) {
            this.resources = resources;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_firestore_resource, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            FirestoreResource resource = resources.get(position);

            holder.textFileIcon.setText(resource.getFileTypeIcon());
            holder.textResourceTitle.setText(resource.getTitle());
            holder.textResourceDescription.setText(resource.getDescription());
            holder.textResourceCategory.setText(resource.getCategory() != null ? resource.getCategory() : "Other");
            holder.textFileType.setText(resource.getFileType() != null ? resource.getFileType().toUpperCase() : "FILE");

            // Download/Open button
            holder.btnDownload.setOnClickListener(v -> {
                String url = resource.getFileUrl();
                if (url != null && !url.isEmpty()) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                } else {
                    Toast.makeText(getContext(), "No file URL available", Toast.LENGTH_SHORT).show();
                }
            });

            // Share button
            holder.btnShare.setOnClickListener(v -> {
                String url = resource.getFileUrl();
                if (url != null && !url.isEmpty()) {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, resource.getTitle());
                    shareIntent.putExtra(Intent.EXTRA_TEXT,
                            resource.getTitle() + "\n\n" + resource.getDescription() + "\n\n" + url);
                    startActivity(Intent.createChooser(shareIntent, "Share Resource"));
                } else {
                    Toast.makeText(getContext(), "No file URL available to share", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public int getItemCount() {
            return resources.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textFileIcon, textResourceTitle, textResourceDescription;
            TextView textResourceCategory, textFileType;
            Button btnDownload;
            ImageButton btnShare;

            ViewHolder(View itemView) {
                super(itemView);
                textFileIcon = itemView.findViewById(R.id.textFileIcon);
                textResourceTitle = itemView.findViewById(R.id.textResourceTitle);
                textResourceDescription = itemView.findViewById(R.id.textResourceDescription);
                textResourceCategory = itemView.findViewById(R.id.textResourceCategory);
                textFileType = itemView.findViewById(R.id.textFileType);
                btnDownload = itemView.findViewById(R.id.btnDownload);
                btnShare = itemView.findViewById(R.id.btnShare);
            }
        }
    }
}
