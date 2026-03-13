package com.example.fblaapp;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fblaapp.data.AuthRepository;
import com.example.fblaapp.data.ReminderEntity;
import com.example.fblaapp.data.ReminderRepository;
import com.example.fblaapp.data.UserEntity;
import com.example.fblaapp.utils.ReminderScheduler;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import com.example.fblaapp.utils.AppExecutors;

public class EventDetailActivity extends AppCompatActivity {

    private TextView textTitle;
    private TextView textStartDateTime;
    private TextView textEndDateTime;
    private TextView textLocation;
    private TextView textDescription;
    private MaterialCardView cardLocation;
    private MaterialCardView cardDescription;
    private ImageButton btnBack;
    private ImageButton btnEdit;

    // Reminder views
    private SwitchMaterial switchReminder;
    private LinearLayout layoutReminderOptions;
    private RadioGroup radioGroupReminder;
    private RadioButton radioOneHour;
    private RadioButton radioOneDay;
    private RadioButton radioCustom;
    private LinearLayout layoutCustomTime;
    private TextView textCustomDate;
    private TextView textCustomTime;
    private Button btnSaveReminder;
    private TextView textReminderStatus;

    private final SimpleDateFormat fullDateFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy • h:mm a", Locale.US);
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.US);
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM d, yyyy", Locale.US);
    private final SimpleDateFormat statusFormat = new SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.US);

    // Event data
    private long eventId;
    private String eventTitle;
    private String eventDescription;
    private String eventLocation;
    private long eventStart;
    private long eventEnd;
    private boolean isOfficer;

    // Reminder data
    private AuthRepository authRepository;
    private ReminderRepository reminderRepository;
    private ReminderScheduler reminderScheduler;
    private ReminderEntity currentReminder;
    private Calendar customReminderCalendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        // Initialize repositories using getInstance()
        authRepository = AuthRepository.getInstance(this);
        reminderRepository = ReminderRepository.getInstance(this);
        reminderScheduler = new ReminderScheduler(this);
        customReminderCalendar = Calendar.getInstance();

        initViews();
        loadEventData();
        displayEvent();
        setupClickListeners();
        loadReminder();
    }

    private void initViews() {
        textTitle = findViewById(R.id.textTitle);
        textStartDateTime = findViewById(R.id.textStartDateTime);
        textEndDateTime = findViewById(R.id.textEndDateTime);
        textLocation = findViewById(R.id.textLocation);
        textDescription = findViewById(R.id.textDescription);
        cardLocation = findViewById(R.id.cardLocation);
        cardDescription = findViewById(R.id.cardDescription);
        btnBack = findViewById(R.id.btnBack);
        btnEdit = findViewById(R.id.btnEdit);

        // Reminder views
        switchReminder = findViewById(R.id.switchReminder);
        layoutReminderOptions = findViewById(R.id.layoutReminderOptions);
        radioGroupReminder = findViewById(R.id.radioGroupReminder);
        radioOneHour = findViewById(R.id.radioOneHour);
        radioOneDay = findViewById(R.id.radioOneDay);
        radioCustom = findViewById(R.id.radioCustom);
        layoutCustomTime = findViewById(R.id.layoutCustomTime);
        textCustomDate = findViewById(R.id.textCustomDate);
        textCustomTime = findViewById(R.id.textCustomTime);
        btnSaveReminder = findViewById(R.id.btnSaveReminder);
        textReminderStatus = findViewById(R.id.textReminderStatus);
    }

    private void loadEventData() {
        Intent intent = getIntent();
        eventId = intent.getLongExtra("event_id", -1);
        eventTitle = intent.getStringExtra("event_title");
        eventDescription = intent.getStringExtra("event_description");
        eventLocation = intent.getStringExtra("event_location");
        eventStart = intent.getLongExtra("event_start", System.currentTimeMillis());
        eventEnd = intent.getLongExtra("event_end", System.currentTimeMillis());
        isOfficer = intent.getBooleanExtra("is_officer", false);

        // Set default custom reminder time (1 hour before event)
        customReminderCalendar.setTimeInMillis(eventStart);
        customReminderCalendar.add(Calendar.HOUR_OF_DAY, -1);
    }

    private void displayEvent() {
        // Title
        textTitle.setText(eventTitle != null ? eventTitle : "Untitled Event");

        // Date/Time
        textStartDateTime.setText(fullDateFormat.format(new Date(eventStart)));
        
        // Check if same day
        Date startDate = new Date(eventStart);
        Date endDate = new Date(eventEnd);
        SimpleDateFormat dayCheck = new SimpleDateFormat("yyyyMMdd", Locale.US);
        
        if (dayCheck.format(startDate).equals(dayCheck.format(endDate))) {
            // Same day - just show end time
            textEndDateTime.setText("to " + timeFormat.format(endDate));
        } else {
            // Different days - show full end date/time
            textEndDateTime.setText("to " + fullDateFormat.format(endDate));
        }

        // Location
        if (eventLocation != null && !eventLocation.trim().isEmpty()) {
            cardLocation.setVisibility(View.VISIBLE);
            textLocation.setText(eventLocation);
        } else {
            cardLocation.setVisibility(View.GONE);
        }

        // Description
        if (eventDescription != null && !eventDescription.trim().isEmpty()) {
            cardDescription.setVisibility(View.VISIBLE);
            textDescription.setText(eventDescription);
        } else {
            cardDescription.setVisibility(View.GONE);
        }

        // Edit button (officers only)
        if (isOfficer) {
            btnEdit.setVisibility(View.VISIBLE);
        } else {
            btnEdit.setVisibility(View.GONE);
        }
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddEditEventActivity.class);
            intent.putExtra("event_id", eventId);
            intent.putExtra("event_title", eventTitle);
            intent.putExtra("event_description", eventDescription);
            intent.putExtra("event_location", eventLocation);
            intent.putExtra("event_start", eventStart);
            intent.putExtra("event_end", eventEnd);
            startActivity(intent);
            overridePendingTransition(0, 0);
            finish();
        });

        // Reminder switch
        switchReminder.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                layoutReminderOptions.setVisibility(View.VISIBLE);
                textReminderStatus.setVisibility(View.GONE);
            } else {
                layoutReminderOptions.setVisibility(View.GONE);
                // Cancel existing reminder
                if (currentReminder != null && currentReminder.isEnabled()) {
                    cancelReminder();
                }
            }
        });

        // Radio group for reminder options
        radioGroupReminder.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioCustom) {
                layoutCustomTime.setVisibility(View.VISIBLE);
                updateCustomTimeDisplay();
            } else {
                layoutCustomTime.setVisibility(View.GONE);
            }
        });

        // Custom date picker
        textCustomDate.setOnClickListener(v -> showDatePicker());

        // Custom time picker
        textCustomTime.setOnClickListener(v -> showTimePicker());

        // Save reminder button
        btnSaveReminder.setOnClickListener(v -> saveReminder());
    }

    private void showDatePicker() {
        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    customReminderCalendar.set(Calendar.YEAR, year);
                    customReminderCalendar.set(Calendar.MONTH, month);
                    customReminderCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateCustomTimeDisplay();
                },
                customReminderCalendar.get(Calendar.YEAR),
                customReminderCalendar.get(Calendar.MONTH),
                customReminderCalendar.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    private void showTimePicker() {
        TimePickerDialog dialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    customReminderCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    customReminderCalendar.set(Calendar.MINUTE, minute);
                    updateCustomTimeDisplay();
                },
                customReminderCalendar.get(Calendar.HOUR_OF_DAY),
                customReminderCalendar.get(Calendar.MINUTE),
                false
        );
        dialog.show();
    }

    private void updateCustomTimeDisplay() {
        textCustomDate.setText(dateFormat.format(customReminderCalendar.getTime()));
        textCustomTime.setText(timeFormat.format(customReminderCalendar.getTime()));
    }

    private void loadReminder() {
        UserEntity currentUser = authRepository.getCurrentUser();
        if (currentUser == null) return;

        AppExecutors.diskIO().execute(() -> {
            ReminderEntity reminder = reminderRepository.getReminderForEvent(currentUser.getId(), eventId);
            runOnUiThread(() -> {
                currentReminder = reminder;
                if (reminder != null && reminder.isEnabled()) {
                    switchReminder.setChecked(true);
                    layoutReminderOptions.setVisibility(View.VISIBLE);
                    
                    // Show current reminder status
                    textReminderStatus.setText(getString(R.string.reminder_set, 
                            statusFormat.format(new Date(reminder.getRemindAtMillis()))));
                    textReminderStatus.setVisibility(View.VISIBLE);

                    // Select appropriate radio button based on saved time
                    long oneHourBefore = ReminderScheduler.calculateReminderTime(eventStart, 0);
                    long oneDayBefore = ReminderScheduler.calculateReminderTime(eventStart, 1);

                    if (Math.abs(reminder.getRemindAtMillis() - oneHourBefore) < 60000) {
                        radioOneHour.setChecked(true);
                    } else if (Math.abs(reminder.getRemindAtMillis() - oneDayBefore) < 60000) {
                        radioOneDay.setChecked(true);
                    } else {
                        radioCustom.setChecked(true);
                        customReminderCalendar.setTimeInMillis(reminder.getRemindAtMillis());
                        updateCustomTimeDisplay();
                        layoutCustomTime.setVisibility(View.VISIBLE);
                    }
                } else {
                    switchReminder.setChecked(false);
                    layoutReminderOptions.setVisibility(View.GONE);
                }
            });
        });
    }

    private void saveReminder() {
        long remindAtMillis;

        int checkedId = radioGroupReminder.getCheckedRadioButtonId();
        if (checkedId == R.id.radioOneHour) {
            remindAtMillis = ReminderScheduler.calculateReminderTime(eventStart, 0);
        } else if (checkedId == R.id.radioOneDay) {
            remindAtMillis = ReminderScheduler.calculateReminderTime(eventStart, 1);
        } else {
            remindAtMillis = customReminderCalendar.getTimeInMillis();
        }

        // Validate reminder time
        if (remindAtMillis <= System.currentTimeMillis()) {
            Toast.makeText(this, "Reminder time has already passed", Toast.LENGTH_SHORT).show();
            return;
        }

        if (remindAtMillis >= eventStart) {
            Toast.makeText(this, "Reminder must be before the event", Toast.LENGTH_SHORT).show();
            return;
        }

        UserEntity currentUser = authRepository.getCurrentUser();
        if (currentUser == null) return;

        final long finalRemindAtMillis = remindAtMillis;
        AppExecutors.diskIO().execute(() -> {
            try {
                ReminderEntity reminder = reminderRepository.setReminder(
                        eventId, currentUser.getId(), finalRemindAtMillis, true);
                
                runOnUiThread(() -> {
                    currentReminder = reminder;
                    
                    // Schedule the alarm
                    reminderScheduler.scheduleReminder(reminder, eventTitle, eventStart);
                    
                    // Update status
                    textReminderStatus.setText(getString(R.string.reminder_set, 
                            statusFormat.format(new Date(reminder.getRemindAtMillis()))));
                    textReminderStatus.setVisibility(View.VISIBLE);
                    
                    Toast.makeText(EventDetailActivity.this, R.string.reminder_saved, Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(EventDetailActivity.this, R.string.reminder_error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void cancelReminder() {
        if (currentReminder != null) {
            // Cancel the alarm
            reminderScheduler.cancelReminder(currentReminder.getId());

            AppExecutors.diskIO().execute(() -> {
                reminderRepository.disableReminder(currentReminder.getId());
                runOnUiThread(() -> {
                    textReminderStatus.setVisibility(View.GONE);
                    Toast.makeText(EventDetailActivity.this, R.string.reminder_cancelled, Toast.LENGTH_SHORT).show();
                });
            });
        }
    }
}
