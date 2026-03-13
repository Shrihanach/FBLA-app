package com.example.fblaapp;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fblaapp.data.AuthRepository;
import com.example.fblaapp.data.EventEntity;
import com.example.fblaapp.data.EventRepository;
import com.example.fblaapp.data.UserEntity;
import com.example.fblaapp.utils.AppExecutors;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CalendarActivity extends AppCompatActivity {

    private RecyclerView rvEvents;
    private RecyclerView recyclerCalendar;
    private FloatingActionButton fabAddEvent;
    private LinearLayout layoutEmpty;
    private TextView textRoleInfo;
    private TextView textEmptyHint;
    private TextView textSelectedDate;
    private TextView textEventsHeader;
    private TextView textShowAll;
    private TextView textMonthYear;
    private ImageButton btnPrevMonth;
    private ImageButton btnNextMonth;

    // Inline event panel
    private MaterialCardView cardCalendar;
    private MaterialCardView inlineEventPanel;
    private FrameLayout overlayDim;
    private EditText editInlineTitle;
    private EditText editInlineDescription;
    private EditText editInlineLocation;
    private TextView textInlineStartDate;
    private TextView textInlineStartTime;
    private TextView textInlineEndDate;
    private TextView textInlineEndTime;
    private TextView textInlineError;
    private TextView btnInlineAddTime;
    private LinearLayout layoutInlineTimeRow;
    private Button btnSaveInline;
    private ImageButton btnClosePanel;
    private boolean isPanelOpen = false;
    private boolean inlineTimeVisible = false;
    private Calendar inlineStartCalendar;
    private Calendar inlineEndCalendar;
    private final SimpleDateFormat inlineDateFormat = new SimpleDateFormat("EEEE, MMMM d", Locale.US);
    private final SimpleDateFormat inlineTimeFormat = new SimpleDateFormat("h:mm a", Locale.US);

    private EventsAdapter adapter;
    private CalendarDayAdapter calendarDayAdapter;
    private AuthRepository authRepository;
    private EventRepository eventRepository;
    private boolean isOfficer = false;

    private List<EventEntity> allEvents = new ArrayList<>();
    private Long selectedDateMillis = null;

    // Calendar state
    private Calendar displayedMonth; // The month currently shown in the calendar
    private Set<String> eventDayKeys = new HashSet<>(); // Set of "yyyyMMdd" strings that have events

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.US);
    private final SimpleDateFormat dayFormat = new SimpleDateFormat("yyyyMMdd", Locale.US);
    private final SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        // Initialize repositories
        authRepository = AuthRepository.getInstance(this);
        eventRepository = EventRepository.getInstance(this);

        // Check login
        checkLoginStatus();

        // Initialize views
        initViews();

        // Check user role
        UserEntity currentUser = authRepository.getCurrentUser();
        if (currentUser != null) {
            isOfficer = currentUser.isOfficer();

            if (isOfficer) {
                fabAddEvent.setVisibility(View.VISIBLE);
                textRoleInfo.setText(currentUser.getRoleDisplayName() + " - You can manage events");
            } else {
                fabAddEvent.setVisibility(View.GONE);
                textRoleInfo.setText("Member - View upcoming events");
            }
        }

        // Setup RecyclerView for events
        setupRecyclerView();

        // Setup custom calendar
        setupCustomCalendar();

        // Setup FAB — open inline panel with morph animation
        fabAddEvent.setOnClickListener(v -> showInlineEventPanel());

        // Setup inline panel buttons
        setupInlinePanel();

        // Setup Show All button
        textShowAll.setOnClickListener(v -> {
            selectedDateMillis = null;
            textSelectedDate.setText("All Events");
            textEventsHeader.setText("Upcoming Events");
            textShowAll.setVisibility(View.GONE);
            calendarDayAdapter.setSelectedDay(-1);
            filterEvents();
        });

        // Setup bottom navigation
        setupBottomNavigation();

        // Observe events
        observeEvents();

        // Handle system back using OnBackPressedDispatcher
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isPanelOpen) {
                    hideInlineEventPanel();
                } else {
                    // Default back behavior: close this screen
                    finish();
                }
            }
        });
    }

    private void initViews() {
        rvEvents = findViewById(R.id.rvEvents);
        recyclerCalendar = findViewById(R.id.recyclerCalendar);
        fabAddEvent = findViewById(R.id.fabAddEvent);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        textRoleInfo = findViewById(R.id.textRoleInfo);
        textEmptyHint = findViewById(R.id.textEmptyHint);
        textSelectedDate = findViewById(R.id.textSelectedDate);
        textEventsHeader = findViewById(R.id.textEventsHeader);
        textShowAll = findViewById(R.id.textShowAll);
        textMonthYear = findViewById(R.id.textMonthYear);
        btnPrevMonth = findViewById(R.id.btnPrevMonth);
        btnNextMonth = findViewById(R.id.btnNextMonth);

        // Inline event panel views
        cardCalendar = findViewById(R.id.cardCalendar);
        inlineEventPanel = findViewById(R.id.inlineEventPanel);
        overlayDim = findViewById(R.id.overlayDim);
        editInlineTitle = findViewById(R.id.editInlineTitle);
        editInlineDescription = findViewById(R.id.editInlineDescription);
        editInlineLocation = findViewById(R.id.editInlineLocation);
        textInlineStartDate = findViewById(R.id.textInlineStartDate);
        textInlineStartTime = findViewById(R.id.textInlineStartTime);
        textInlineEndDate = findViewById(R.id.textInlineEndDate);
        textInlineEndTime = findViewById(R.id.textInlineEndTime);
        textInlineError = findViewById(R.id.textInlineError);
        btnInlineAddTime = findViewById(R.id.btnInlineAddTime);
        layoutInlineTimeRow = findViewById(R.id.layoutInlineTimeRow);
        btnSaveInline = findViewById(R.id.btnSaveInline);
        btnClosePanel = findViewById(R.id.btnClosePanel);
    }

    private void checkLoginStatus() {
        if (authRepository.getCurrentUser() == null) {
            Intent intent = new Intent(this, Login.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    // ==================== Custom Calendar ====================

    private void setupCustomCalendar() {
        displayedMonth = Calendar.getInstance();
        displayedMonth.set(Calendar.DAY_OF_MONTH, 1);

        calendarDayAdapter = new CalendarDayAdapter();
        recyclerCalendar.setLayoutManager(new GridLayoutManager(this, 7));
        recyclerCalendar.setAdapter(calendarDayAdapter);

        // Month navigation
        btnPrevMonth.setOnClickListener(v -> {
            displayedMonth.add(Calendar.MONTH, -1);
            updateCalendarGrid();
        });

        btnNextMonth.setOnClickListener(v -> {
            displayedMonth.add(Calendar.MONTH, 1);
            updateCalendarGrid();
        });

        updateCalendarGrid();
    }

    private void updateCalendarGrid() {
        // Update header
        textMonthYear.setText(monthYearFormat.format(displayedMonth.getTime()));

        // Build the list of day cells for this month
        List<DayCell> days = buildDayCells();
        calendarDayAdapter.setDays(days);
    }

    private List<DayCell> buildDayCells() {
        List<DayCell> cells = new ArrayList<>();

        Calendar cal = (Calendar) displayedMonth.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);

        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK); // 1=Sun, 7=Sat
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        // Today for comparison
        Calendar today = Calendar.getInstance();
        String todayKey = dayFormat.format(today.getTime());

        // Blank cells before the 1st
        int blanks = firstDayOfWeek - 1; // Sunday=1, so 0 blanks if month starts on Sunday
        for (int i = 0; i < blanks; i++) {
            cells.add(new DayCell(0, false, false, false)); // empty cell
        }

        // Day cells
        for (int day = 1; day <= daysInMonth; day++) {
            cal.set(Calendar.DAY_OF_MONTH, day);
            String dayKey = dayFormat.format(cal.getTime());

            boolean isToday = dayKey.equals(todayKey);
            boolean hasEvent = eventDayKeys.contains(dayKey);

            cells.add(new DayCell(day, true, isToday, hasEvent));
        }

        return cells;
    }

    private void buildEventDayKeys() {
        eventDayKeys.clear();
        for (EventEntity event : allEvents) {
            String key = dayFormat.format(new Date(event.getStartTimeMillis()));
            eventDayKeys.add(key);
        }
    }

    // ==================== Events ====================

    private void setupRecyclerView() {
        adapter = new EventsAdapter();
        adapter.setOfficer(isOfficer);
        adapter.setOnEventClickListener(new EventsAdapter.OnEventClickListener() {
            @Override
            public void onEventClick(EventEntity event) {
                Intent intent = new Intent(CalendarActivity.this, EventDetailActivity.class);
                intent.putExtra("event_id", event.getId());
                intent.putExtra("event_title", event.getTitle());
                intent.putExtra("event_description", event.getDescription());
                intent.putExtra("event_location", event.getLocation());
                intent.putExtra("event_start", event.getStartTimeMillis());
                intent.putExtra("event_end", event.getEndTimeMillis());
                intent.putExtra("is_officer", isOfficer);
                startActivity(intent);
            }

            @Override
            public void onEditClick(EventEntity event) {
                Intent intent = new Intent(CalendarActivity.this, AddEditEventActivity.class);
                intent.putExtra("event_id", event.getId());
                intent.putExtra("event_title", event.getTitle());
                intent.putExtra("event_description", event.getDescription());
                intent.putExtra("event_location", event.getLocation());
                intent.putExtra("event_start", event.getStartTimeMillis());
                intent.putExtra("event_end", event.getEndTimeMillis());
                intent.putExtra("created_by_user_id", event.getCreatedByUserId());
                startActivity(intent);
                overridePendingTransition(0, 0);
            }

            @Override
            public void onDeleteClick(EventEntity event) {
                showDeleteConfirmation(event);
            }
        });

        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        rvEvents.setAdapter(adapter);
    }

    private void showDeleteConfirmation(EventEntity event) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_event)
                .setMessage(R.string.delete_event_confirm)
                .setPositiveButton(R.string.delete, (dialog, which) -> deleteEvent(event))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void deleteEvent(EventEntity event) {
        AppExecutors.diskIO().execute(() -> {
            try {
                eventRepository.deleteEvent(event);
                runOnUiThread(() ->
                    Toast.makeText(CalendarActivity.this, R.string.event_deleted, Toast.LENGTH_SHORT).show()
                );
            } catch (SecurityException e) {
                runOnUiThread(() ->
                    Toast.makeText(CalendarActivity.this, R.string.only_officers_can_modify, Toast.LENGTH_SHORT).show()
                );
            } catch (Exception e) {
                runOnUiThread(() ->
                    Toast.makeText(CalendarActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void observeEvents() {
        eventRepository.getAllEventsOrderedByStart().observe(this, events -> {
            allEvents = events != null ? events : new ArrayList<>();
            buildEventDayKeys();
            updateCalendarGrid(); // Refresh dots
            filterEvents();
        });
    }

    private void filterEvents() {
        List<EventEntity> filteredEvents;

        if (selectedDateMillis != null) {
            // Filter events for selected date
            filteredEvents = new ArrayList<>();
            Calendar selectedDay = Calendar.getInstance();
            selectedDay.setTimeInMillis(selectedDateMillis);
            String selectedDayStr = dayFormat.format(selectedDay.getTime());

            for (EventEntity event : allEvents) {
                String eventDayStr = dayFormat.format(new Date(event.getStartTimeMillis()));
                if (eventDayStr.equals(selectedDayStr)) {
                    filteredEvents.add(event);
                }
            }
        } else {
            // Show all upcoming events (from today onwards)
            filteredEvents = new ArrayList<>();
            long now = System.currentTimeMillis();

            for (EventEntity event : allEvents) {
                if (event.getEndTimeMillis() >= now) {
                    filteredEvents.add(event);
                }
            }
        }

        adapter.setEvents(filteredEvents);
        updateEmptyState(filteredEvents);
    }

    private void updateEmptyState(List<EventEntity> events) {
        if (events == null || events.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvEvents.setVisibility(View.GONE);

            if (selectedDateMillis != null) {
                textEmptyHint.setText("No events on this date");
            } else if (isOfficer) {
                textEmptyHint.setText("Tap the + button to add your first event");
            } else {
                textEmptyHint.setText("Check back later for upcoming events");
            }
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvEvents.setVisibility(View.VISIBLE);
        }
    }

    // ==================== Inline Event Panel ====================

    private void setupInlinePanel() {
        inlineStartCalendar = Calendar.getInstance();
        inlineEndCalendar = Calendar.getInstance();
        inlineEndCalendar.add(Calendar.HOUR_OF_DAY, 1);

        btnClosePanel.setOnClickListener(v -> hideInlineEventPanel());
        overlayDim.setOnClickListener(v -> hideInlineEventPanel());

        btnSaveInline.setOnClickListener(v -> saveInlineEvent());

        // Date pickers
        textInlineStartDate.setOnClickListener(v -> {
            DatePickerDialog dialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    inlineStartCalendar.set(Calendar.YEAR, year);
                    inlineStartCalendar.set(Calendar.MONTH, month);
                    inlineStartCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    if (inlineStartCalendar.after(inlineEndCalendar)) {
                        inlineEndCalendar.setTimeInMillis(inlineStartCalendar.getTimeInMillis());
                        inlineEndCalendar.add(Calendar.HOUR_OF_DAY, 1);
                    }
                    updateInlineDateTimeDisplay();
                },
                inlineStartCalendar.get(Calendar.YEAR),
                inlineStartCalendar.get(Calendar.MONTH),
                inlineStartCalendar.get(Calendar.DAY_OF_MONTH));
            dialog.show();
        });

        textInlineEndDate.setOnClickListener(v -> {
            DatePickerDialog dialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    inlineEndCalendar.set(Calendar.YEAR, year);
                    inlineEndCalendar.set(Calendar.MONTH, month);
                    inlineEndCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateInlineDateTimeDisplay();
                },
                inlineEndCalendar.get(Calendar.YEAR),
                inlineEndCalendar.get(Calendar.MONTH),
                inlineEndCalendar.get(Calendar.DAY_OF_MONTH));
            dialog.show();
        });

        // Time pickers
        textInlineStartTime.setOnClickListener(v -> {
            TimePickerDialog dialog = new TimePickerDialog(this,
                (view, hourOfDay, minute) -> {
                    inlineStartCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    inlineStartCalendar.set(Calendar.MINUTE, minute);
                    if (inlineStartCalendar.after(inlineEndCalendar)) {
                        inlineEndCalendar.setTimeInMillis(inlineStartCalendar.getTimeInMillis());
                        inlineEndCalendar.add(Calendar.HOUR_OF_DAY, 1);
                    }
                    updateInlineDateTimeDisplay();
                },
                inlineStartCalendar.get(Calendar.HOUR_OF_DAY),
                inlineStartCalendar.get(Calendar.MINUTE), false);
            dialog.show();
        });

        textInlineEndTime.setOnClickListener(v -> {
            TimePickerDialog dialog = new TimePickerDialog(this,
                (view, hourOfDay, minute) -> {
                    inlineEndCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    inlineEndCalendar.set(Calendar.MINUTE, minute);
                    updateInlineDateTimeDisplay();
                },
                inlineEndCalendar.get(Calendar.HOUR_OF_DAY),
                inlineEndCalendar.get(Calendar.MINUTE), false);
            dialog.show();
        });

        // Add time toggle
        btnInlineAddTime.setOnClickListener(v -> {
            if (!inlineTimeVisible) {
                inlineTimeVisible = true;
                layoutInlineTimeRow.setVisibility(View.VISIBLE);
                btnInlineAddTime.setText("Remove time");
            } else {
                inlineTimeVisible = false;
                layoutInlineTimeRow.setVisibility(View.GONE);
                btnInlineAddTime.setText("Add time");
            }
        });
    }

    private void updateInlineDateTimeDisplay() {
        textInlineStartDate.setText(inlineDateFormat.format(inlineStartCalendar.getTime()));
        textInlineEndDate.setText(inlineDateFormat.format(inlineEndCalendar.getTime()));
        textInlineStartTime.setText(inlineTimeFormat.format(inlineStartCalendar.getTime()));
        textInlineEndTime.setText(inlineTimeFormat.format(inlineEndCalendar.getTime()));
    }

    private void showInlineEventPanel() {
        if (isPanelOpen) return;
        isPanelOpen = true;

        // Reset form
        editInlineTitle.setText("");
        editInlineDescription.setText("");
        editInlineLocation.setText("");
        textInlineError.setVisibility(View.GONE);
        inlineTimeVisible = false;
        layoutInlineTimeRow.setVisibility(View.GONE);
        btnInlineAddTime.setText("Add time");
        inlineStartCalendar = Calendar.getInstance();
        inlineEndCalendar = Calendar.getInstance();
        inlineEndCalendar.add(Calendar.HOUR_OF_DAY, 1);
        updateInlineDateTimeDisplay();

        // Get calendar card position
        Rect calRect = new Rect();
        cardCalendar.getGlobalVisibleRect(calRect);

        // Show panel at calendar card position initially, then morph
        inlineEventPanel.setVisibility(View.VISIBLE);
        overlayDim.setVisibility(View.VISIBLE);
        overlayDim.setAlpha(0f);

        inlineEventPanel.post(() -> {
            // Get panel's final position
            Rect panelRect = new Rect();
            inlineEventPanel.getGlobalVisibleRect(panelRect);

            float panelW = panelRect.width();
            float panelH = panelRect.height();

            // Scale from calendar card size
            float scaleX = (float) calRect.width() / panelW;
            float scaleY = (float) calRect.height() / panelH;

            // Pivot at center so it expands outward from the card center
            inlineEventPanel.setPivotX(panelW / 2f);
            inlineEventPanel.setPivotY(panelH / 2f);

            // Translate so the panel's center aligns with the calendar card's center
            float calCenterX = calRect.centerX();
            float calCenterY = calRect.centerY();
            float panelCenterX = panelRect.centerX();
            float panelCenterY = panelRect.centerY();
            float startTX = calCenterX - panelCenterX;
            float startTY = calCenterY - panelCenterY;

            inlineEventPanel.setScaleX(scaleX);
            inlineEventPanel.setScaleY(scaleY);
            inlineEventPanel.setTranslationX(startTX);
            inlineEventPanel.setTranslationY(startTY);

            // Corner radius morph
            float density = getResources().getDisplayMetrics().density;
            float calRadius = 16f * density;
            float panelRadius = 20f * density;
            inlineEventPanel.setRadius(calRadius);

            // Hide form content, fade it in after expansion starts
            View formContent = ((android.view.ViewGroup) inlineEventPanel).getChildAt(0);
            formContent.setAlpha(0f);

            ObjectAnimator contentFade = ObjectAnimator.ofFloat(formContent, "alpha", 0f, 1f);
            contentFade.setStartDelay(120);

            AnimatorSet set = new AnimatorSet();
            set.playTogether(
                ObjectAnimator.ofFloat(inlineEventPanel, "translationX", startTX, 0f),
                ObjectAnimator.ofFloat(inlineEventPanel, "translationY", startTY, 0f),
                ObjectAnimator.ofFloat(inlineEventPanel, "scaleX", scaleX, 1f),
                ObjectAnimator.ofFloat(inlineEventPanel, "scaleY", scaleY, 1f),
                ObjectAnimator.ofFloat(inlineEventPanel, "radius", calRadius, panelRadius),
                ObjectAnimator.ofFloat(overlayDim, "alpha", 0f, 1f),
                ObjectAnimator.ofFloat(cardCalendar, "alpha", 1f, 0f),
                contentFade
            );
            set.setDuration(350);
            set.setInterpolator(new DecelerateInterpolator(2f));
            set.start();

            // Hide FAB during panel
            fabAddEvent.hide();
        });
    }

    private void hideInlineEventPanel() {
        if (!isPanelOpen) return;

        Rect calRect = new Rect();
        cardCalendar.getGlobalVisibleRect(calRect);

        Rect panelRect = new Rect();
        inlineEventPanel.getGlobalVisibleRect(panelRect);

        float panelW = panelRect.width();
        float panelH = panelRect.height();

        float scaleX = (float) calRect.width() / panelW;
        float scaleY = (float) calRect.height() / panelH;

        // Translate panel center back to calendar card center
        float endTX = calRect.centerX() - panelRect.centerX();
        float endTY = calRect.centerY() - panelRect.centerY();

        float density = getResources().getDisplayMetrics().density;
        float calRadius = 16f * density;
        float panelRadius = inlineEventPanel.getRadius();

        // Fade out form content first
        View formContent = ((android.view.ViewGroup) inlineEventPanel).getChildAt(0);

        ObjectAnimator contentFade = ObjectAnimator.ofFloat(formContent, "alpha", 1f, 0f);
        contentFade.setDuration(100);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(
            ObjectAnimator.ofFloat(inlineEventPanel, "translationX", 0f, endTX),
            ObjectAnimator.ofFloat(inlineEventPanel, "translationY", 0f, endTY),
            ObjectAnimator.ofFloat(inlineEventPanel, "scaleX", 1f, scaleX),
            ObjectAnimator.ofFloat(inlineEventPanel, "scaleY", 1f, scaleY),
            ObjectAnimator.ofFloat(inlineEventPanel, "radius", panelRadius, calRadius),
            ObjectAnimator.ofFloat(overlayDim, "alpha", 1f, 0f),
            ObjectAnimator.ofFloat(cardCalendar, "alpha", 0f, 1f),
            contentFade
        );
        set.setDuration(300);
        set.setInterpolator(new AccelerateInterpolator(1.5f));
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                isPanelOpen = false;
                inlineEventPanel.setVisibility(View.GONE);
                overlayDim.setVisibility(View.GONE);
                inlineEventPanel.setTranslationX(0f);
                inlineEventPanel.setTranslationY(0f);
                inlineEventPanel.setScaleX(1f);
                inlineEventPanel.setScaleY(1f);
                inlineEventPanel.setAlpha(1f);
                formContent.setAlpha(1f);
                fabAddEvent.show();
            }
        });
        set.start();
    }

    private void saveInlineEvent() {
        String title = editInlineTitle.getText() != null ? editInlineTitle.getText().toString().trim() : "";
        String description = editInlineDescription.getText() != null ? editInlineDescription.getText().toString().trim() : "";
        String location = editInlineLocation.getText() != null ? editInlineLocation.getText().toString().trim() : "";

        if (title.isEmpty()) {
            textInlineError.setText(getString(R.string.title_required));
            textInlineError.setVisibility(View.VISIBLE);
            editInlineTitle.requestFocus();
            return;
        }

        if (inlineEndCalendar.getTimeInMillis() <= inlineStartCalendar.getTimeInMillis()) {
            textInlineError.setText(getString(R.string.end_after_start));
            textInlineError.setVisibility(View.VISIBLE);
            return;
        }

        textInlineError.setVisibility(View.GONE);
        long userId = eventRepository.getCurrentUserId();

        AppExecutors.diskIO().execute(() -> {
            try {
                EventEntity event = new EventEntity(
                    title,
                    description.isEmpty() ? null : description,
                    location.isEmpty() ? null : location,
                    inlineStartCalendar.getTimeInMillis(),
                    inlineEndCalendar.getTimeInMillis(),
                    userId
                );
                eventRepository.createEvent(event);

                runOnUiThread(() -> {
                    Toast.makeText(CalendarActivity.this, R.string.event_added, Toast.LENGTH_SHORT).show();
                    hideInlineEventPanel();
                });
            } catch (SecurityException e) {
                runOnUiThread(() -> Toast.makeText(CalendarActivity.this, R.string.only_officers_can_modify, Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                runOnUiThread(() -> {
                    textInlineError.setText("Error: " + e.getMessage());
                    textInlineError.setVisibility(View.VISIBLE);
                });
            }
        });
    }

    // ==================== Bottom Navigation ====================

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setSelectedItemId(R.id.nav_calendar);

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                Intent i = new Intent(this, HomeActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(i);
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                return true;
            } else if (itemId == R.id.nav_calendar) {
                return true;
            } else if (itemId == R.id.nav_resources) {
                Intent i = new Intent(this, ResourcesActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(i);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                return true;
            } else if (itemId == R.id.nav_social) {
                Intent i = new Intent(this, SocialActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(i);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                return true;
            } else if (itemId == R.id.nav_profile) {
                Intent i = new Intent(this, ProfileActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(i);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setSelectedItemId(R.id.nav_calendar);
    }

    // ==================== Day Cell Model ====================

    private static class DayCell {
        final int day;          // 0 = blank cell
        final boolean isValid;  // true if it's a real day
        final boolean isToday;
        final boolean hasEvent;

        DayCell(int day, boolean isValid, boolean isToday, boolean hasEvent) {
            this.day = day;
            this.isValid = isValid;
            this.isToday = isToday;
            this.hasEvent = hasEvent;
        }
    }

    // ==================== Calendar Day Adapter ====================

    private class CalendarDayAdapter extends RecyclerView.Adapter<CalendarDayAdapter.DayViewHolder> {

        private List<DayCell> days = new ArrayList<>();
        private int selectedDayNumber = -1; // -1 = no selection

        void setDays(List<DayCell> days) {
            this.days = days;
            notifyDataSetChanged();
        }

        void setSelectedDay(int dayNumber) {
            this.selectedDayNumber = dayNumber;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_calendar_day, parent, false);
            return new DayViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
            DayCell cell = days.get(position);

            if (!cell.isValid) {
                // Blank cell
                holder.textDay.setText("");
                holder.textDay.setBackground(null);
                holder.dotIndicator.setVisibility(View.INVISIBLE);
                holder.itemView.setOnClickListener(null);
                holder.itemView.setClickable(false);
                return;
            }

            holder.textDay.setText(String.valueOf(cell.day));
            holder.itemView.setClickable(true);

            // Determine background and text color
            boolean isSelected = (cell.day == selectedDayNumber);

            if (isSelected) {
                holder.textDay.setBackgroundResource(R.drawable.selected_day_background);
                holder.textDay.setTextColor(getResources().getColor(R.color.white, null));
            } else if (cell.isToday) {
                holder.textDay.setBackgroundResource(R.drawable.today_day_background);
                holder.textDay.setTextColor(getResources().getColor(R.color.cobalt, null));
            } else {
                holder.textDay.setBackground(null);
                holder.textDay.setTextColor(getResources().getColor(R.color.text_primary, null));
            }

            // Event dot
            if (cell.hasEvent) {
                holder.dotIndicator.setVisibility(View.VISIBLE);
            } else {
                holder.dotIndicator.setVisibility(View.INVISIBLE);
            }

            // Click to select a day
            holder.itemView.setOnClickListener(v -> {
                Calendar cal = (Calendar) displayedMonth.clone();
                cal.set(Calendar.DAY_OF_MONTH, cell.day);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);

                selectedDateMillis = cal.getTimeInMillis();
                selectedDayNumber = cell.day;

                String formattedDate = dateFormat.format(cal.getTime());
                textSelectedDate.setText(formattedDate);
                textEventsHeader.setText("Events on " + formattedDate);
                textShowAll.setVisibility(View.VISIBLE);

                notifyDataSetChanged();
                filterEvents();
            });
        }

        @Override
        public int getItemCount() {
            return days.size();
        }

        class DayViewHolder extends RecyclerView.ViewHolder {
            TextView textDay;
            View dotIndicator;

            DayViewHolder(View itemView) {
                super(itemView);
                textDay = itemView.findViewById(R.id.textDay);
                dotIndicator = itemView.findViewById(R.id.dotIndicator);
            }
        }
    }
}
