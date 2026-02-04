package com.example.fblaapp;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class CalendarActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "FBLAConnectPrefs";
    private static final String KEY_EVENTS = "events";
    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");

    // Event colors
    private static final String COLOR_BLUE = "#4A90D9";
    private static final String COLOR_GREEN = "#2ECC71";
    private static final String COLOR_ORANGE = "#E67E22";
    private static final String COLOR_PURPLE = "#9B59B6";
    private static final String COLOR_TEAL = "#1ABC9C";

    private TextView textMonthYear;
    private ImageButton btnPrevMonth, btnNextMonth;
    private RecyclerView recyclerCalendar;
    private FloatingActionButton fabAddEvent;
    private BottomNavigationView bottomNavigation;

    private SharedPreferences prefs;
    private List<Event> events;
    private Calendar currentCalendar;
    private CalendarAdapter adapter;
    private String selectedColor = COLOR_BLUE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        currentCalendar = Calendar.getInstance();

        initViews();
        loadEvents();
        setupCalendar();
        setupListeners();
        setupBottomNavigation();
    }

    private void initViews() {
        textMonthYear = findViewById(R.id.textMonthYear);
        btnPrevMonth = findViewById(R.id.btnPrevMonth);
        btnNextMonth = findViewById(R.id.btnNextMonth);
        recyclerCalendar = findViewById(R.id.recyclerCalendar);
        fabAddEvent = findViewById(R.id.fabAddEvent);
        bottomNavigation = findViewById(R.id.bottomNavigation);
    }

    private void loadEvents() {
        events = new ArrayList<>();
        String eventsJson = prefs.getString(KEY_EVENTS, null);

        if (eventsJson != null) {
            try {
                JSONArray jsonArray = new JSONArray(eventsJson);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    events.add(new Event(
                            obj.getString("title"),
                            obj.getString("date"),
                            obj.optString("time", ""),
                            obj.optString("color", COLOR_BLUE)
                    ));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveEvents() {
        try {
            JSONArray jsonArray = new JSONArray();
            for (Event event : events) {
                JSONObject obj = new JSONObject();
                obj.put("title", event.title);
                obj.put("date", event.date);
                obj.put("time", event.time);
                obj.put("color", event.color);
                jsonArray.put(obj);
            }
            prefs.edit().putString(KEY_EVENTS, jsonArray.toString()).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void setupCalendar() {
        recyclerCalendar.setLayoutManager(new GridLayoutManager(this, 7));
        updateCalendarView();
    }

    private void updateCalendarView() {
        // Update month/year text
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        textMonthYear.setText(sdf.format(currentCalendar.getTime()));

        // Generate days for the month
        List<DayCell> dayCells = generateDaysForMonth();
        adapter = new CalendarAdapter(dayCells, this::onDayClicked);
        recyclerCalendar.setAdapter(adapter);
    }

    private void onDayClicked(DayCell dayCell) {
        if (dayCell.day > 0) {
            showAddEventDialog(dayCell.dateStr);
        }
    }

    private List<DayCell> generateDaysForMonth() {
        List<DayCell> days = new ArrayList<>();

        Calendar cal = (Calendar) currentCalendar.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);

        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1; // 0 = Sunday
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        // Get current date for highlighting
        Calendar today = Calendar.getInstance();
        int currentDay = today.get(Calendar.DAY_OF_MONTH);
        int currentMonth = today.get(Calendar.MONTH);
        int currentYear = today.get(Calendar.YEAR);

        // Add empty cells for days before the first day of the month
        for (int i = 0; i < firstDayOfWeek; i++) {
            days.add(new DayCell(0, "", false, new ArrayList<>()));
        }

        // Add days of the month
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);

        for (int day = 1; day <= daysInMonth; day++) {
            String dateStr = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day);
            List<Event> dayEvents = getEventsForDate(dateStr);
            boolean isToday = (day == currentDay && month == currentMonth && year == currentYear);
            days.add(new DayCell(day, dateStr, isToday, dayEvents));
        }

        // Add empty cells to complete the grid (6 rows × 7 columns = 42)
        while (days.size() < 42) {
            days.add(new DayCell(0, "", false, new ArrayList<>()));
        }

        return days;
    }

    private List<Event> getEventsForDate(String date) {
        List<Event> dayEvents = new ArrayList<>();
        for (Event event : events) {
            if (event.date.equals(date)) {
                dayEvents.add(event);
            }
        }
        return dayEvents;
    }

    private void setupListeners() {
        btnPrevMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, -1);
            updateCalendarView();
        });

        btnNextMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, 1);
            updateCalendarView();
        });

        fabAddEvent.setOnClickListener(v -> {
            // Use current calendar date
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            showAddEventDialog(sdf.format(Calendar.getInstance().getTime()));
        });
    }

    private void showAddEventDialog(String prefilledDate) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_add_event);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        TextView dialogTitle = dialog.findViewById(R.id.dialogTitle);
        TextView textSelectedDate = dialog.findViewById(R.id.textSelectedDate);
        TextInputEditText editTitle = dialog.findViewById(R.id.editEventTitle);
        TextInputEditText editDate = dialog.findViewById(R.id.editEventDate);
        TextInputEditText editTime = dialog.findViewById(R.id.editEventTime);
        TextView textError = dialog.findViewById(R.id.textError);
        Button btnCancel = dialog.findViewById(R.id.btnCancel);
        Button btnSave = dialog.findViewById(R.id.btnSave);

        // Color selection
        View colorBlue = dialog.findViewById(R.id.colorBlue);
        View colorGreen = dialog.findViewById(R.id.colorGreen);
        View colorOrange = dialog.findViewById(R.id.colorOrange);
        View colorPurple = dialog.findViewById(R.id.colorPurple);
        View colorTeal = dialog.findViewById(R.id.colorTeal);

        selectedColor = COLOR_BLUE;
        updateColorSelection(colorBlue, colorGreen, colorOrange, colorPurple, colorTeal);

        colorBlue.setOnClickListener(v -> {
            selectedColor = COLOR_BLUE;
            updateColorSelection(colorBlue, colorGreen, colorOrange, colorPurple, colorTeal);
        });
        colorGreen.setOnClickListener(v -> {
            selectedColor = COLOR_GREEN;
            updateColorSelection(colorBlue, colorGreen, colorOrange, colorPurple, colorTeal);
        });
        colorOrange.setOnClickListener(v -> {
            selectedColor = COLOR_ORANGE;
            updateColorSelection(colorBlue, colorGreen, colorOrange, colorPurple, colorTeal);
        });
        colorPurple.setOnClickListener(v -> {
            selectedColor = COLOR_PURPLE;
            updateColorSelection(colorBlue, colorGreen, colorOrange, colorPurple, colorTeal);
        });
        colorTeal.setOnClickListener(v -> {
            selectedColor = COLOR_TEAL;
            updateColorSelection(colorBlue, colorGreen, colorOrange, colorPurple, colorTeal);
        });

        // Pre-fill date
        editDate.setText(prefilledDate);

        // Format the selected date for display
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            SimpleDateFormat outputFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US);
            textSelectedDate.setText(outputFormat.format(inputFormat.parse(prefilledDate)));
        } catch (ParseException e) {
            textSelectedDate.setText(prefilledDate);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String title = editTitle.getText().toString().trim();
            String date = editDate.getText().toString().trim();
            String time = editTime.getText().toString().trim();

            // Validation: Title must be 3+ characters
            if (title.length() < 3) {
                textError.setText(R.string.title_min_chars);
                textError.setVisibility(View.VISIBLE);
                return;
            }

            // Validation: Date format YYYY-MM-DD
            if (!isValidDate(date)) {
                textError.setText(R.string.invalid_date_format);
                textError.setVisibility(View.VISIBLE);
                return;
            }

            // Add event
            Event newEvent = new Event(title, date, time, selectedColor);
            events.add(newEvent);
            saveEvents();
            updateCalendarView();

            Toast.makeText(this, R.string.event_added, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void updateColorSelection(View blue, View green, View orange, View purple, View teal) {
        float selectedScale = 1.2f;
        float normalScale = 1.0f;
        
        blue.setScaleX(selectedColor.equals(COLOR_BLUE) ? selectedScale : normalScale);
        blue.setScaleY(selectedColor.equals(COLOR_BLUE) ? selectedScale : normalScale);
        blue.setAlpha(selectedColor.equals(COLOR_BLUE) ? 1.0f : 0.6f);
        
        green.setScaleX(selectedColor.equals(COLOR_GREEN) ? selectedScale : normalScale);
        green.setScaleY(selectedColor.equals(COLOR_GREEN) ? selectedScale : normalScale);
        green.setAlpha(selectedColor.equals(COLOR_GREEN) ? 1.0f : 0.6f);
        
        orange.setScaleX(selectedColor.equals(COLOR_ORANGE) ? selectedScale : normalScale);
        orange.setScaleY(selectedColor.equals(COLOR_ORANGE) ? selectedScale : normalScale);
        orange.setAlpha(selectedColor.equals(COLOR_ORANGE) ? 1.0f : 0.6f);
        
        purple.setScaleX(selectedColor.equals(COLOR_PURPLE) ? selectedScale : normalScale);
        purple.setScaleY(selectedColor.equals(COLOR_PURPLE) ? selectedScale : normalScale);
        purple.setAlpha(selectedColor.equals(COLOR_PURPLE) ? 1.0f : 0.6f);
        
        teal.setScaleX(selectedColor.equals(COLOR_TEAL) ? selectedScale : normalScale);
        teal.setScaleY(selectedColor.equals(COLOR_TEAL) ? selectedScale : normalScale);
        teal.setAlpha(selectedColor.equals(COLOR_TEAL) ? 1.0f : 0.6f);
    }

    private boolean isValidDate(String date) {
        if (!DATE_PATTERN.matcher(date).matches()) {
            return false;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        sdf.setLenient(false);
        try {
            sdf.parse(date);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_calendar);
        bottomNavigation.setOnItemSelectedListener(item -> {
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

    // Callback interface for day click
    interface OnDayClickListener {
        void onDayClick(DayCell dayCell);
    }

    // Data classes
    static class Event {
        String title;
        String date;
        String time;
        String color;

        Event(String title, String date, String time, String color) {
            this.title = title;
            this.date = date;
            this.time = time;
            this.color = color;
        }
    }

    static class DayCell {
        int day;
        String dateStr;
        boolean isToday;
        List<Event> events;

        DayCell(int day, String dateStr, boolean isToday, List<Event> events) {
            this.day = day;
            this.dateStr = dateStr;
            this.isToday = isToday;
            this.events = events;
        }
    }

    // Calendar Adapter
    class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.ViewHolder> {
        private final List<DayCell> days;
        private final OnDayClickListener clickListener;

        CalendarAdapter(List<DayCell> days, OnDayClickListener clickListener) {
            this.days = days;
            this.clickListener = clickListener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_calendar_day, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            DayCell dayCell = days.get(position);

            if (dayCell.day == 0) {
                // Empty cell
                holder.textDayNumber.setText("");
                holder.textDayNumber.setBackground(null);
                holder.eventsContainer.removeAllViews();
                holder.itemView.setOnClickListener(null);
                holder.itemView.setClickable(false);
            } else {
                holder.textDayNumber.setText(String.valueOf(dayCell.day));

                // Highlight today
                if (dayCell.isToday) {
                    holder.textDayNumber.setBackgroundResource(R.drawable.current_day_circle);
                    holder.textDayNumber.setTextColor(Color.WHITE);
                } else {
                    holder.textDayNumber.setBackground(null);
                    // Set Sunday text color to red
                    if (position % 7 == 0) {
                        holder.textDayNumber.setTextColor(Color.parseColor("#E74C3C"));
                    } else {
                        holder.textDayNumber.setTextColor(ContextCompat.getColor(CalendarActivity.this, R.color.text_primary));
                    }
                }

                // Add events
                holder.eventsContainer.removeAllViews();
                int maxEvents = Math.min(dayCell.events.size(), 3); // Show max 3 events
                for (int i = 0; i < maxEvents; i++) {
                    Event event = dayCell.events.get(i);
                    TextView eventView = new TextView(CalendarActivity.this);

                    String displayText = event.time.isEmpty() ? event.title : event.time + " " + event.title;
                    eventView.setText(displayText);
                    eventView.setTextColor(Color.WHITE);
                    eventView.setTextSize(9);
                    eventView.setMaxLines(1);
                    eventView.setPadding(6, 3, 6, 3);

                    // Create colored background
                    GradientDrawable bg = new GradientDrawable();
                    bg.setColor(Color.parseColor(event.color));
                    bg.setCornerRadius(8);
                    eventView.setBackground(bg);

                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    params.bottomMargin = 2;
                    eventView.setLayoutParams(params);

                    // Long press to delete
                    int eventIndex = events.indexOf(event);
                    eventView.setOnLongClickListener(v -> {
                        if (eventIndex >= 0) {
                            events.remove(eventIndex);
                            saveEvents();
                            updateCalendarView();
                            Toast.makeText(CalendarActivity.this, R.string.event_deleted, Toast.LENGTH_SHORT).show();
                        }
                        return true;
                    });

                    holder.eventsContainer.addView(eventView);
                }

                // Show "+X more" if there are more events
                if (dayCell.events.size() > 3) {
                    TextView moreView = new TextView(CalendarActivity.this);
                    moreView.setText("+" + (dayCell.events.size() - 3) + " more");
                    moreView.setTextColor(ContextCompat.getColor(CalendarActivity.this, R.color.text_secondary));
                    moreView.setTextSize(9);
                    holder.eventsContainer.addView(moreView);
                }

                // Set click listener on the day cell
                holder.itemView.setClickable(true);
                holder.itemView.setOnClickListener(v -> {
                    if (clickListener != null) {
                        clickListener.onDayClick(dayCell);
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return days.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textDayNumber;
            LinearLayout eventsContainer;

            ViewHolder(View itemView) {
                super(itemView);
                textDayNumber = itemView.findViewById(R.id.textDayNumber);
                eventsContainer = itemView.findViewById(R.id.eventsContainer);
            }
        }
    }
}
