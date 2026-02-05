package com.example.fblaapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CalendarView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fblaapp.data.AuthRepository;
import com.example.fblaapp.data.EventEntity;
import com.example.fblaapp.data.EventRepository;
import com.example.fblaapp.data.UserEntity;
import com.example.fblaapp.utils.AppExecutors;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CalendarActivity extends AppCompatActivity {

    private RecyclerView rvEvents;
    private FloatingActionButton fabAddEvent;
    private LinearLayout layoutEmpty;
    private TextView textRoleInfo;
    private TextView textEmptyHint;
    private CalendarView calendarView;
    private TextView textSelectedDate;
    private TextView textEventsHeader;
    private TextView textShowAll;

    private EventsAdapter adapter;
    private AuthRepository authRepository;
    private EventRepository eventRepository;
    private boolean isOfficer = false;

    private List<EventEntity> allEvents = new ArrayList<>();
    private Long selectedDateMillis = null;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.US);
    private final SimpleDateFormat dayFormat = new SimpleDateFormat("yyyyMMdd", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        // Initialize repositories using getInstance()
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
                textRoleInfo.setText("Officer - You can manage events");
            } else {
                fabAddEvent.setVisibility(View.GONE);
                textRoleInfo.setText("Member - View upcoming events");
            }
        }

        // Setup RecyclerView
        setupRecyclerView();

        // Setup Calendar
        setupCalendar();

        // Setup FAB
        fabAddEvent.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddEditEventActivity.class);
            startActivity(intent);
        });

        // Setup Show All button
        textShowAll.setOnClickListener(v -> {
            selectedDateMillis = null;
            textSelectedDate.setText("All Events");
            textEventsHeader.setText("Upcoming Events");
            textShowAll.setVisibility(View.GONE);
            filterEvents();
        });

        // Setup bottom navigation
        setupBottomNavigation();

        // Observe events
        observeEvents();
    }

    private void initViews() {
        rvEvents = findViewById(R.id.rvEvents);
        fabAddEvent = findViewById(R.id.fabAddEvent);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        textRoleInfo = findViewById(R.id.textRoleInfo);
        textEmptyHint = findViewById(R.id.textEmptyHint);
        calendarView = findViewById(R.id.calendarView);
        textSelectedDate = findViewById(R.id.textSelectedDate);
        textEventsHeader = findViewById(R.id.textEventsHeader);
        textShowAll = findViewById(R.id.textShowAll);
    }

    private void checkLoginStatus() {
        if (authRepository.getCurrentUser() == null) {
            Intent intent = new Intent(this, Login.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    private void setupCalendar() {
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, dayOfMonth, 0, 0, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            
            selectedDateMillis = calendar.getTimeInMillis();
            
            String formattedDate = dateFormat.format(calendar.getTime());
            textSelectedDate.setText(formattedDate);
            textEventsHeader.setText("Events on " + formattedDate);
            textShowAll.setVisibility(View.VISIBLE);
            
            filterEvents();
        });
    }

    private void setupRecyclerView() {
        adapter = new EventsAdapter();
        adapter.setOfficer(isOfficer);
        adapter.setOnEventClickListener(new EventsAdapter.OnEventClickListener() {
            @Override
            public void onEventClick(EventEntity event) {
                // Open detail view
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
                // Open edit activity
                Intent intent = new Intent(CalendarActivity.this, AddEditEventActivity.class);
                intent.putExtra("event_id", event.getId());
                intent.putExtra("event_title", event.getTitle());
                intent.putExtra("event_description", event.getDescription());
                intent.putExtra("event_location", event.getLocation());
                intent.putExtra("event_start", event.getStartTimeMillis());
                intent.putExtra("event_end", event.getEndTimeMillis());
                intent.putExtra("created_by_user_id", event.getCreatedByUserId());
                startActivity(intent);
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
        eventRepository.getAllEventsOrderedByStart().observe(this, new Observer<List<EventEntity>>() {
            @Override
            public void onChanged(List<EventEntity> events) {
                allEvents = events != null ? events : new ArrayList<>();
                filterEvents();
            }
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
                // Check if event starts on selected day
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

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setSelectedItemId(R.id.nav_calendar);
        
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_calendar) {
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
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setSelectedItemId(R.id.nav_calendar);
    }
}
