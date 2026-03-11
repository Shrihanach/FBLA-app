package com.example.fblaapp;

import android.app.Dialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

    // ── Attachment state (shared across add / edit dialog) ──
    private JSONObject pendingAttachments;
    private LinearLayout activeChipsContainer;
    private Dialog activeDialog;

    // Activity result launchers for file and audio picking
    private ActivityResultLauncher<Intent> filePickerLauncher;
    private ActivityResultLauncher<Intent> audioPickerLauncher;

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

        // Register activity result launchers (must be before onStart)
        initActivityResultLaunchers();

        initViews();
        setupUserRole();
        setupRecyclerView();
        setupBottomNavigation();
        loadAnnouncements();

        fabAddAnnouncement.setOnClickListener(v -> showAddAnnouncementDialog());
    }

    // ==================== Activity Result Launchers ====================

    private void initActivityResultLaunchers() {
        filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri fileUri = result.getData().getData();
                    if (fileUri != null) {
                        handleFileAttachment(fileUri);
                    }
                }
            }
        );

        audioPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri audioUri = result.getData().getData();
                    if (audioUri != null) {
                        handleAudioAttachment(audioUri);
                    }
                }
            }
        );
    }

    // ==================== View Setup ====================

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

    // ==================== Announcements Loading ====================

    private void loadAnnouncements() {
        currentDisplayCount = INITIAL_LOAD_COUNT;
        announcementRepository.getAllAnnouncementsLive().observe(this, announcements -> {
            allAnnouncements = announcements != null ? announcements : new ArrayList<>();
            updateDisplayedAnnouncements();
        });
    }

    private void updateDisplayedAnnouncements() {
        List<AnnouncementEntity> displayList;
        boolean showMore;

        if (allAnnouncements.size() > currentDisplayCount) {
            displayList = allAnnouncements.subList(0, currentDisplayCount);
            showMore = true;
        } else {
            displayList = new ArrayList<>(allAnnouncements);
            showMore = false;
        }

        adapter.setAnnouncements(displayList);
        adapter.setShowMoreVisible(showMore);

        if (allAnnouncements.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            recyclerAnnouncements.setVisibility(View.GONE);
            textEmptyHint.setText(isOfficer
                    ? "Tap the + button to post your first announcement"
                    : "Check back later for announcements");
        } else {
            layoutEmpty.setVisibility(View.GONE);
            recyclerAnnouncements.setVisibility(View.VISIBLE);
        }
    }

    private void loadMoreAnnouncements() {
        currentDisplayCount += INITIAL_LOAD_COUNT;
        updateDisplayedAnnouncements();
    }

    // ==================== Add / Edit Dialogs ====================

    private void showAddAnnouncementDialog() {
        pendingAttachments = new JSONObject();

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_add_announcement);
        dialog.getWindow().setLayout(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        activeDialog = dialog;

        TextView dialogTitle = dialog.findViewById(R.id.dialogTitle);
        dialogTitle.setText("New Announcement");

        TextInputEditText editTitle = dialog.findViewById(R.id.editTitle);
        TextInputEditText editContent = dialog.findViewById(R.id.editContent);
        Button btnCancel = dialog.findViewById(R.id.btnCancel);
        Button btnPost = dialog.findViewById(R.id.btnPost);

        activeChipsContainer = dialog.findViewById(R.id.layoutAttachmentChips);

        // Setup formatting toolbar
        setupFormattingToolbar(dialog, editContent);
        // Setup attachment bar
        setupAttachmentBar(dialog);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnPost.setOnClickListener(v -> {
            String title = editTitle.getText() != null ? editTitle.getText().toString().trim() : "";
            String htmlContent = getHtmlFromEditText(editContent);

            if (title.isEmpty()) {
                editTitle.setError("Title is required");
                editTitle.requestFocus();
                return;
            }

            if (htmlContent.isEmpty()) {
                editContent.setError("Content is required");
                editContent.requestFocus();
                return;
            }

            String attachJson = pendingAttachments.length() > 0 ? pendingAttachments.toString() : null;
            postAnnouncement(title, htmlContent, attachJson, dialog);
        });

        dialog.show();
    }

    private void showEditAnnouncementDialog(AnnouncementEntity announcement) {
        // Parse existing attachments
        pendingAttachments = new JSONObject();
        if (announcement.getAttachmentJson() != null) {
            try {
                pendingAttachments = new JSONObject(announcement.getAttachmentJson());
            } catch (JSONException ignored) {}
        }

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_add_announcement);
        dialog.getWindow().setLayout(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        activeDialog = dialog;

        TextView dialogTitle = dialog.findViewById(R.id.dialogTitle);
        dialogTitle.setText("Edit Announcement");

        TextInputEditText editTitle = dialog.findViewById(R.id.editTitle);
        TextInputEditText editContent = dialog.findViewById(R.id.editContent);
        Button btnCancel = dialog.findViewById(R.id.btnCancel);
        Button btnPost = dialog.findViewById(R.id.btnPost);

        activeChipsContainer = dialog.findViewById(R.id.layoutAttachmentChips);

        // Pre-fill with existing data (render HTML back to styled text)
        editTitle.setText(announcement.getTitle());
        editContent.setText(Html.fromHtml(announcement.getContent(), Html.FROM_HTML_MODE_COMPACT));
        btnPost.setText("Save");

        // Setup formatting toolbar
        setupFormattingToolbar(dialog, editContent);
        // Setup attachment bar
        setupAttachmentBar(dialog);
        // Re-render existing attachment chips
        refreshAttachmentChips();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnPost.setOnClickListener(v -> {
            String title = editTitle.getText() != null ? editTitle.getText().toString().trim() : "";
            String htmlContent = getHtmlFromEditText(editContent);

            if (title.isEmpty()) {
                editTitle.setError("Title is required");
                editTitle.requestFocus();
                return;
            }

            if (htmlContent.isEmpty()) {
                editContent.setError("Content is required");
                editContent.requestFocus();
                return;
            }

            String attachJson = pendingAttachments.length() > 0 ? pendingAttachments.toString() : null;
            updateAnnouncement(announcement.getId(), title, htmlContent, attachJson, dialog);
        });

        dialog.show();
    }

    // ==================== Rich Text Formatting ====================

    private void setupFormattingToolbar(Dialog dialog, TextInputEditText editContent) {
        ImageButton btnBold = dialog.findViewById(R.id.btnFormatBold);
        ImageButton btnUnderline = dialog.findViewById(R.id.btnFormatUnderline);
        ImageButton btnIndent = dialog.findViewById(R.id.btnFormatIndent);
        ImageButton btnBullet = dialog.findViewById(R.id.btnFormatBulletList);
        ImageButton btnNumber = dialog.findViewById(R.id.btnFormatNumberList);

        btnBold.setOnClickListener(v -> toggleBold(editContent));
        btnUnderline.setOnClickListener(v -> toggleUnderline(editContent));
        btnIndent.setOnClickListener(v -> applyIndent(editContent));
        btnBullet.setOnClickListener(v -> insertBulletList(editContent));
        btnNumber.setOnClickListener(v -> insertNumberedList(editContent));
    }

    private void toggleBold(EditText editText) {
        Editable text = editText.getText();
        if (text == null) return;

        int start = editText.getSelectionStart();
        int end = editText.getSelectionEnd();

        if (start == end) {
            Toast.makeText(this, "Select text to bold", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if already bold
        StyleSpan[] existing = text.getSpans(start, end, StyleSpan.class);
        boolean alreadyBold = false;
        for (StyleSpan span : existing) {
            if (span.getStyle() == Typeface.BOLD) {
                text.removeSpan(span);
                alreadyBold = true;
            }
        }

        if (!alreadyBold) {
            text.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private void toggleUnderline(EditText editText) {
        Editable text = editText.getText();
        if (text == null) return;

        int start = editText.getSelectionStart();
        int end = editText.getSelectionEnd();

        if (start == end) {
            Toast.makeText(this, "Select text to underline", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if already underlined
        UnderlineSpan[] existing = text.getSpans(start, end, UnderlineSpan.class);
        if (existing.length > 0) {
            for (UnderlineSpan span : existing) {
                text.removeSpan(span);
            }
        } else {
            text.setSpan(new UnderlineSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private void applyIndent(EditText editText) {
        Editable text = editText.getText();
        if (text == null) return;

        int start = editText.getSelectionStart();

        // Find start of current line
        int lineStart = start;
        while (lineStart > 0 && text.charAt(lineStart - 1) != '\n') {
            lineStart--;
        }

        // Insert indent (4 spaces)
        text.insert(lineStart, "    ");
    }

    private void insertBulletList(EditText editText) {
        Editable text = editText.getText();
        if (text == null) return;

        int start = editText.getSelectionStart();

        // Find start of current line
        int lineStart = start;
        while (lineStart > 0 && text.charAt(lineStart - 1) != '\n') {
            lineStart--;
        }

        // Check if line already starts with bullet
        String linePrefix = text.subSequence(lineStart,
                Math.min(lineStart + 2, text.length())).toString();

        if (linePrefix.startsWith("• ")) {
            // Remove bullet
            text.delete(lineStart, lineStart + 2);
        } else {
            // Add bullet
            text.insert(lineStart, "• ");
        }
    }

    private void insertNumberedList(EditText editText) {
        Editable text = editText.getText();
        if (text == null) return;

        int start = editText.getSelectionStart();

        // Find start of current line
        int lineStart = start;
        while (lineStart > 0 && text.charAt(lineStart - 1) != '\n') {
            lineStart--;
        }

        // Check if line already starts with a number prefix
        String currentText = text.toString();
        String afterLineStart = currentText.substring(lineStart);

        if (afterLineStart.matches("^\\d+\\. .*") || afterLineStart.matches("^\\d+\\. ?$")) {
            // Remove existing number prefix
            int dotSpace = afterLineStart.indexOf(". ");
            if (dotSpace >= 0) {
                text.delete(lineStart, lineStart + dotSpace + 2);
            }
        } else {
            // Count previous numbered lines to determine the next number
            int number = 1;
            int searchPos = lineStart - 1;
            while (searchPos > 0) {
                // Find previous line
                int prevLineEnd = searchPos;
                int prevLineStart = searchPos;
                while (prevLineStart > 0 && currentText.charAt(prevLineStart - 1) != '\n') {
                    prevLineStart--;
                }
                String prevLine = currentText.substring(prevLineStart, prevLineEnd + 1);
                if (prevLine.matches("^\\d+\\. .*")) {
                    try {
                        number = Integer.parseInt(prevLine.substring(0, prevLine.indexOf("."))) + 1;
                    } catch (NumberFormatException ignored) {}
                    break;
                }
                searchPos = prevLineStart - 1;
                if (searchPos < 0) break;
            }

            text.insert(lineStart, number + ". ");
        }
    }

    /** Convert the styled EditText content to an HTML string for storage. */
    private String getHtmlFromEditText(EditText editText) {
        if (editText.getText() == null) return "";
        // Use Html.toHtml to preserve bold/underline spans
        String html = Html.toHtml(new SpannableStringBuilder(editText.getText()), Html.TO_HTML_PARAGRAPH_LINES_INDIVIDUAL);
        // Strip wrapping tags that Html.toHtml adds
        if (html == null) return "";
        return html.trim();
    }

    // ==================== Attachment Handling ====================

    private void setupAttachmentBar(Dialog dialog) {
        dialog.findViewById(R.id.btnAttachFile).setOnClickListener(v -> openFilePicker());
        dialog.findViewById(R.id.btnAttachLink).setOnClickListener(v -> showAddLinkDialog());
        dialog.findViewById(R.id.btnAttachAudio).setOnClickListener(v -> openAudioPicker());
        dialog.findViewById(R.id.btnAttachPoll).setOnClickListener(v -> showCreatePollDialog());
    }

    // ── File Attachment ──

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        filePickerLauncher.launch(intent);
    }

    private void handleFileAttachment(Uri fileUri) {
        String fileName = getFileNameFromUri(fileUri);
        // Copy file to internal storage
        String savedPath = copyUriToInternal(fileUri, "file_" + System.currentTimeMillis());
        if (savedPath == null) {
            Toast.makeText(this, "Failed to attach file", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONArray files = pendingAttachments.optJSONArray("files");
            if (files == null) files = new JSONArray();
            JSONObject fileObj = new JSONObject();
            fileObj.put("name", fileName);
            fileObj.put("path", savedPath);
            files.put(fileObj);
            pendingAttachments.put("files", files);
            refreshAttachmentChips();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // ── Link Attachment ──

    private void showAddLinkDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Link");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 32, 48, 16);

        EditText editUrl = new EditText(this);
        editUrl.setHint("https://example.com");
        editUrl.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_URI);
        layout.addView(editUrl);

        EditText editLinkTitle = new EditText(this);
        editLinkTitle.setHint("Link title (optional)");
        editLinkTitle.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        layout.addView(editLinkTitle);

        builder.setView(layout);

        builder.setPositiveButton("Add", (d, which) -> {
            String url = editUrl.getText().toString().trim();
            String title = editLinkTitle.getText().toString().trim();
            if (url.isEmpty()) {
                Toast.makeText(this, "URL is required", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }
            try {
                JSONArray links = pendingAttachments.optJSONArray("links");
                if (links == null) links = new JSONArray();
                JSONObject linkObj = new JSONObject();
                linkObj.put("url", url);
                linkObj.put("title", title.isEmpty() ? url : title);
                links.put(linkObj);
                pendingAttachments.put("links", links);
                refreshAttachmentChips();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // ── Audio Attachment ──

    private void openAudioPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*");
        audioPickerLauncher.launch(intent);
    }

    private void handleAudioAttachment(Uri audioUri) {
        String fileName = getFileNameFromUri(audioUri);
        String savedPath = copyUriToInternal(audioUri, "audio_" + System.currentTimeMillis());
        if (savedPath == null) {
            Toast.makeText(this, "Failed to attach audio", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONArray audios = pendingAttachments.optJSONArray("audios");
            if (audios == null) audios = new JSONArray();
            JSONObject audioObj = new JSONObject();
            audioObj.put("name", fileName);
            audioObj.put("path", savedPath);
            audios.put(audioObj);
            pendingAttachments.put("audios", audios);
            refreshAttachmentChips();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // ── Poll Creation ──

    private void showCreatePollDialog() {
        // Check if a poll already exists
        if (pendingAttachments.has("poll")) {
            Toast.makeText(this, "Only one poll per announcement", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create Poll");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 32, 48, 16);

        EditText editQuestion = new EditText(this);
        editQuestion.setHint("Poll question");
        editQuestion.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        layout.addView(editQuestion);

        // Option fields (start with 2, max 4)
        List<EditText> optionFields = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            EditText editOption = new EditText(this);
            editOption.setHint("Option " + (i + 1) + (i >= 2 ? " (optional)" : ""));
            editOption.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
            layout.addView(editOption);
            optionFields.add(editOption);
        }

        builder.setView(layout);

        builder.setPositiveButton("Create", (d, which) -> {
            String question = editQuestion.getText().toString().trim();
            if (question.isEmpty()) {
                Toast.makeText(this, "Question is required", Toast.LENGTH_SHORT).show();
                return;
            }

            JSONArray options = new JSONArray();
            for (EditText field : optionFields) {
                String opt = field.getText().toString().trim();
                if (!opt.isEmpty()) {
                    options.put(opt);
                }
            }

            if (options.length() < 2) {
                Toast.makeText(this, "At least 2 options required", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                JSONObject poll = new JSONObject();
                poll.put("question", question);
                poll.put("options", options);
                pendingAttachments.put("poll", poll);
                refreshAttachmentChips();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // ── Attachment Chips ──

    private void refreshAttachmentChips() {
        if (activeChipsContainer == null) return;
        activeChipsContainer.removeAllViews();

        boolean hasContent = false;

        // Files
        JSONArray files = pendingAttachments.optJSONArray("files");
        if (files != null && files.length() > 0) {
            hasContent = true;
            for (int i = 0; i < files.length(); i++) {
                JSONObject f = files.optJSONObject(i);
                if (f == null) continue;
                int idx = i;
                addChipView("📎 " + f.optString("name", "File"),
                        v -> { removeFromArray("files", idx); refreshAttachmentChips(); });
            }
        }

        // Links
        JSONArray links = pendingAttachments.optJSONArray("links");
        if (links != null && links.length() > 0) {
            hasContent = true;
            for (int i = 0; i < links.length(); i++) {
                JSONObject l = links.optJSONObject(i);
                if (l == null) continue;
                int idx = i;
                addChipView("🔗 " + l.optString("title", "Link"),
                        v -> { removeFromArray("links", idx); refreshAttachmentChips(); });
            }
        }

        // Audios
        JSONArray audios = pendingAttachments.optJSONArray("audios");
        if (audios != null && audios.length() > 0) {
            hasContent = true;
            for (int i = 0; i < audios.length(); i++) {
                JSONObject a = audios.optJSONObject(i);
                if (a == null) continue;
                int idx = i;
                addChipView("🎤 " + a.optString("name", "Audio"),
                        v -> { removeFromArray("audios", idx); refreshAttachmentChips(); });
            }
        }

        // Poll
        JSONObject poll = pendingAttachments.optJSONObject("poll");
        if (poll != null) {
            hasContent = true;
            addChipView("📊 Poll: " + poll.optString("question", ""),
                    v -> { pendingAttachments.remove("poll"); refreshAttachmentChips(); });
        }

        activeChipsContainer.setVisibility(hasContent ? View.VISIBLE : View.GONE);
    }

    private void addChipView(String label, View.OnClickListener removeListener) {
        LinearLayout chip = new LinearLayout(this);
        chip.setOrientation(LinearLayout.HORIZONTAL);
        chip.setGravity(Gravity.CENTER_VERTICAL);
        chip.setPadding(24, 12, 12, 12);
        chip.setBackgroundColor(0xFFE8EDF6);

        LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        chipParams.bottomMargin = 4;
        chip.setLayoutParams(chipParams);

        // Label
        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextSize(13);
        tv.setTextColor(ContextCompat.getColor(this, R.color.navy));
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        tv.setMaxLines(1);
        tv.setEllipsize(android.text.TextUtils.TruncateAt.END);
        chip.addView(tv);

        // Remove button
        TextView remove = new TextView(this);
        remove.setText("✕");
        remove.setTextSize(16);
        remove.setTextColor(0xFF888888);
        remove.setPadding(16, 0, 8, 0);
        remove.setOnClickListener(removeListener);
        chip.addView(remove);

        activeChipsContainer.addView(chip);
    }

    private void removeFromArray(String key, int index) {
        JSONArray arr = pendingAttachments.optJSONArray(key);
        if (arr == null) return;
        JSONArray newArr = new JSONArray();
        for (int i = 0; i < arr.length(); i++) {
            if (i != index) newArr.put(arr.opt(i));
        }
        try {
            if (newArr.length() > 0) {
                pendingAttachments.put(key, newArr);
            } else {
                pendingAttachments.remove(key);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // ── Helper: copy a content URI into internal storage ──

    private String copyUriToInternal(Uri uri, String prefix) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            if (is == null) return null;
            File dir = new File(getFilesDir(), "attachments");
            if (!dir.exists()) dir.mkdirs();
            String extension = "";
            String name = getFileNameFromUri(uri);
            if (name.contains(".")) {
                extension = name.substring(name.lastIndexOf("."));
            }
            File outFile = new File(dir, prefix + extension);
            FileOutputStream fos = new FileOutputStream(outFile);
            byte[] buf = new byte[4096];
            int len;
            while ((len = is.read(buf)) > 0) {
                fos.write(buf, 0, len);
            }
            fos.close();
            is.close();
            return outFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getFileNameFromUri(Uri uri) {
        String name = "attachment";
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) {
                    name = cursor.getString(idx);
                }
            }
        }
        return name;
    }

    // ==================== Post / Update / Delete ====================

    private void postAnnouncement(String title, String content, String attachJson, Dialog dialog) {
        AppExecutors.diskIO().execute(() -> {
            try {
                announcementRepository.createAnnouncement(title, content, attachJson);
                runOnUiThread(() -> {
                    dialog.dismiss();
                    Toast.makeText(this, "Announcement posted!", Toast.LENGTH_SHORT).show();
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

    private void updateAnnouncement(long announcementId, String title, String content,
                                     String attachJson, Dialog dialog) {
        AppExecutors.diskIO().execute(() -> {
            try {
                announcementRepository.updateAnnouncement(announcementId, title, content, attachJson);
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

    // ==================== Navigation ====================

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
