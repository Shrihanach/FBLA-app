package com.example.fblaapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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
                textRoleInfo.setText("Officer - You can manage events");
            } else {
                fabAddEvent.setVisibility(View.GONE);
                textRoleInfo.setText("Member - View upcoming events");
            }
        }

        // Setup RecyclerView for events
        setupRecyclerView();

        // Setup custom calendar
        setupCustomCalendar();

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
            calendarDayAdapter.setSelectedDay(-1);
            filterEvents();
        });

        // Setup bottom navigation
        setupBottomNavigation();

        // Observe events
        observeEvents();
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

    // ==================== Bottom Navigation ====================

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
