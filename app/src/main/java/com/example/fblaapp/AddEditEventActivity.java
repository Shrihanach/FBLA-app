package com.example.fblaapp;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;

import com.example.fblaapp.data.AuthRepository;
import com.example.fblaapp.data.EventEntity;
import com.example.fblaapp.data.EventRepository;
import com.example.fblaapp.data.UserEntity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import com.example.fblaapp.utils.AppExecutors;

public class AddEditEventActivity extends AppCompatActivity {

    private EditText editTitle;
    private EditText editDescription;
    private EditText editLocation;
    private TextView textStartDate;
    private TextView textStartTime;
    private TextView textEndDate;
    private TextView textEndTime;
    private TextView textError;
    private TextView textTitle;
    private Button btnSave;
    private ImageButton btnBack;
    private TextView btnAddTime;
    private LinearLayout layoutTimeRow;
    private MaterialCardView cardContainer;
    private FrameLayout rootFrame;

    private EventRepository eventRepository;

    private Calendar startCalendar;
    private Calendar endCalendar;

    private final SimpleDateFormat dateFormatShort = new SimpleDateFormat("EEEE, MMMM d", Locale.US);
    private final SimpleDateFormat dateFormatFull = new SimpleDateFormat("EEE, MMM d, yyyy", Locale.US);
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.US);

    private boolean timeVisible = false;

    // Edit mode fields
    private boolean isEditMode = false;
    private long eventId = -1;
    private long createdByUserId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_event);

        // Initialize repositories using getInstance()
        AuthRepository authRepository = AuthRepository.getInstance(this);
        eventRepository = EventRepository.getInstance(this);

        // Check if officer
        UserEntity currentUser = authRepository.getCurrentUser();
        if (currentUser == null || !currentUser.isOfficer()) {
            Toast.makeText(this, R.string.only_officers_can_modify, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        initViews();

        // Setup calendars
        startCalendar = Calendar.getInstance();
        endCalendar = Calendar.getInstance();
        endCalendar.add(Calendar.HOUR_OF_DAY, 1); // Default 1 hour duration

        // Check if edit mode
        if (getIntent().hasExtra("event_id")) {
            isEditMode = true;
            eventId = getIntent().getLongExtra("event_id", -1);
            createdByUserId = getIntent().getLongExtra("created_by_user_id", -1);
            loadEventData();
        }

        // Update title
        textTitle.setText(isEditMode ? R.string.edit_event : R.string.add_event);

        // Update date/time displays
        updateDateTimeDisplay();

        // Setup click listeners
        setupClickListeners();

        // Use OnBackPressedDispatcher for back gestures
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });

        // If editing, show the time row since existing events have times
        if (isEditMode) {
            showTimeRow();
        }

        // Animate card in
        animateCardIn();
    }

    private void animateCardIn() {
        cardContainer.post(() -> {
            cardContainer.setTranslationY(cardContainer.getHeight());
            cardContainer.setAlpha(0f);
            cardContainer.setScaleX(0.9f);
            cardContainer.setScaleY(0.9f);

            AnimatorSet set = new AnimatorSet();
            set.playTogether(
                ObjectAnimator.ofFloat(cardContainer, "translationY", cardContainer.getHeight(), 0f),
                ObjectAnimator.ofFloat(cardContainer, "alpha", 0f, 1f),
                ObjectAnimator.ofFloat(cardContainer, "scaleX", 0.9f, 1f),
                ObjectAnimator.ofFloat(cardContainer, "scaleY", 0.9f, 1f)
            );
            set.setDuration(300);
            set.setInterpolator(new DecelerateInterpolator(1.5f));
            set.start();
        });
    }

    private void animateCardOut(Runnable onEnd) {
        AnimatorSet set = new AnimatorSet();
        set.playTogether(
            ObjectAnimator.ofFloat(cardContainer, "translationY", 0f, cardContainer.getHeight()),
            ObjectAnimator.ofFloat(cardContainer, "alpha", 1f, 0f),
            ObjectAnimator.ofFloat(cardContainer, "scaleX", 1f, 0.9f),
            ObjectAnimator.ofFloat(cardContainer, "scaleY", 1f, 0.9f)
        );
        set.setDuration(250);
        set.setInterpolator(new android.view.animation.AccelerateInterpolator(1.5f));
        set.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                onEnd.run();
            }
        });
        set.start();
    }

    private void initViews() {
        editTitle = findViewById(R.id.editTitle);
        editDescription = findViewById(R.id.editDescription);
        editLocation = findViewById(R.id.editLocation);
        textStartDate = findViewById(R.id.textStartDate);
        textStartTime = findViewById(R.id.textStartTime);
        textEndDate = findViewById(R.id.textEndDate);
        textEndTime = findViewById(R.id.textEndTime);
        textError = findViewById(R.id.textError);
        textTitle = findViewById(R.id.textTitle);
        btnSave = findViewById(R.id.btnSave);
        btnBack = findViewById(R.id.btnBack);
        btnAddTime = findViewById(R.id.btnAddTime);
        layoutTimeRow = findViewById(R.id.layoutTimeRow);
        cardContainer = findViewById(R.id.cardContainer);
        rootFrame = findViewById(R.id.main);

        // Dismiss when tapping outside the card
        rootFrame.setOnClickListener(v -> finish());
        cardContainer.setOnClickListener(v -> {
            // consume click so it doesn't dismiss
        });
    }

    private void loadEventData() {
        String title = getIntent().getStringExtra("event_title");
        String description = getIntent().getStringExtra("event_description");
        String location = getIntent().getStringExtra("event_location");
        long startMillis = getIntent().getLongExtra("event_start", System.currentTimeMillis());
        long endMillis = getIntent().getLongExtra("event_end", System.currentTimeMillis() + 3600000);

        editTitle.setText(title);
        editDescription.setText(description);
        editLocation.setText(location);

        startCalendar.setTimeInMillis(startMillis);
        endCalendar.setTimeInMillis(endMillis);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        // Add time button
        btnAddTime.setOnClickListener(v -> {
            if (!timeVisible) {
                showTimeRow();
            } else {
                hideTimeRow();
            }
        });

        // Start date picker
        textStartDate.setOnClickListener(v -> showDatePicker(startCalendar, () -> {
            // If start is after end, adjust end
            if (startCalendar.after(endCalendar)) {
                endCalendar.setTimeInMillis(startCalendar.getTimeInMillis());
                endCalendar.add(Calendar.HOUR_OF_DAY, 1);
            }
            updateDateTimeDisplay();
        }));

        // Start time picker
        textStartTime.setOnClickListener(v -> showTimePicker(startCalendar, () -> {
            if (startCalendar.after(endCalendar)) {
                endCalendar.setTimeInMillis(startCalendar.getTimeInMillis());
                endCalendar.add(Calendar.HOUR_OF_DAY, 1);
            }
            updateDateTimeDisplay();
        }));

        // End date picker
        textEndDate.setOnClickListener(v -> showDatePicker(endCalendar, this::updateDateTimeDisplay));

        // End time picker
        textEndTime.setOnClickListener(v -> showTimePicker(endCalendar, this::updateDateTimeDisplay));

        // Save button
        btnSave.setOnClickListener(v -> saveEvent());
    }

    private void showTimeRow() {
        timeVisible = true;
        layoutTimeRow.setVisibility(View.VISIBLE);
        btnAddTime.setText("Remove time");
    }

    private void hideTimeRow() {
        timeVisible = false;
        layoutTimeRow.setVisibility(View.GONE);
        btnAddTime.setText("Add time");
    }

    private void showDatePicker(Calendar calendar, Runnable onDateSet) {
        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    onDateSet.run();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    private void showTimePicker(Calendar calendar, Runnable onTimeSet) {
        TimePickerDialog dialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    calendar.set(Calendar.MINUTE, minute);
                    onTimeSet.run();
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false
        );
        dialog.show();
    }

    private void updateDateTimeDisplay() {
        textStartDate.setText(dateFormatShort.format(startCalendar.getTime()));
        textEndDate.setText(dateFormatShort.format(endCalendar.getTime()));
        textStartTime.setText(timeFormat.format(startCalendar.getTime()));
        textEndTime.setText(timeFormat.format(endCalendar.getTime()));
    }

    private void saveEvent() {
        String title = editTitle.getText() != null ? editTitle.getText().toString().trim() : "";
        String description = editDescription.getText() != null ? editDescription.getText().toString().trim() : "";
        String location = editLocation.getText() != null ? editLocation.getText().toString().trim() : "";

        // Validate
        if (title.isEmpty()) {
            showError(getString(R.string.title_required));
            editTitle.requestFocus();
            return;
        }

        if (endCalendar.getTimeInMillis() <= startCalendar.getTimeInMillis()) {
            showError(getString(R.string.end_after_start));
            return;
        }

        hideError();

        // Get current user ID
        long userId = eventRepository.getCurrentUserId();

        AppExecutors.diskIO().execute(() -> {
            try {
                if (isEditMode) {
                    // Update existing event
                    EventEntity event = new EventEntity(
                            title,
                            description.isEmpty() ? null : description,
                            location.isEmpty() ? null : location,
                            startCalendar.getTimeInMillis(),
                            endCalendar.getTimeInMillis(),
                            createdByUserId > 0 ? createdByUserId : userId
                    );
                    event.setId(eventId);
                    eventRepository.updateEvent(event);
                    
                    runOnUiThread(() -> {
                        Toast.makeText(AddEditEventActivity.this, R.string.event_updated, Toast.LENGTH_SHORT).show();
                        finish();
                    });
                } else {
                    // Create new event
                    EventEntity event = new EventEntity(
                            title,
                            description.isEmpty() ? null : description,
                            location.isEmpty() ? null : location,
                            startCalendar.getTimeInMillis(),
                            endCalendar.getTimeInMillis(),
                            userId
                    );
                    eventRepository.createEvent(event);
                    
                    runOnUiThread(() -> {
                        Toast.makeText(AddEditEventActivity.this, R.string.event_added, Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }
            } catch (SecurityException e) {
                runOnUiThread(() -> Toast.makeText(AddEditEventActivity.this, R.string.only_officers_can_modify, Toast.LENGTH_SHORT).show());
            } catch (IllegalArgumentException e) {
                runOnUiThread(() -> showError(e.getMessage()));
            } catch (Exception e) {
                runOnUiThread(() -> showError("Error: " + e.getMessage()));
            }
        });
    }

    private void showError(String message) {
        textError.setText(message);
        textError.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        textError.setVisibility(View.GONE);
    }

    private boolean isFinishing = false;

    @Override
    public void finish() {
        if (!isFinishing) {
            isFinishing = true;
            animateCardOut(() -> {
                super.finish();
                overridePendingTransition(0, 0);
            });
        }
    }

}
