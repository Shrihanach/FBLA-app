package com.example.fblaapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class CalendarActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "FBLAConnectPrefs";
    private static final String KEY_EVENTS = "events";
    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");

    private EditText editEventTitle, editEventDate;
    private TextView textError, textNoEvents;
    private Button btnAddEvent;
    private RecyclerView recyclerEvents;
    private BottomNavigationView bottomNavigation;
    private SharedPreferences prefs;
    private List<Event> events;
    private EventAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        initViews();
        loadEvents();
        setupRecyclerView();
        setupListeners();
        setupBottomNavigation();
    }

    private void initViews() {
        editEventTitle = findViewById(R.id.editEventTitle);
        editEventDate = findViewById(R.id.editEventDate);
        textError = findViewById(R.id.textError);
        textNoEvents = findViewById(R.id.textNoEvents);
        btnAddEvent = findViewById(R.id.btnAddEvent);
        recyclerEvents = findViewById(R.id.recyclerEvents);
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
                            obj.getString("date")
                    ));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        // Sort by date
        Collections.sort(events, (e1, e2) -> e1.date.compareTo(e2.date));
    }

    private void saveEvents() {
        try {
            JSONArray jsonArray = new JSONArray();
            for (Event event : events) {
                JSONObject obj = new JSONObject();
                obj.put("title", event.title);
                obj.put("date", event.date);
                jsonArray.put(obj);
            }
            prefs.edit().putString(KEY_EVENTS, jsonArray.toString()).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void setupRecyclerView() {
        recyclerEvents.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EventAdapter(events);
        recyclerEvents.setAdapter(adapter);
        updateEmptyState();
    }

    private void setupListeners() {
        btnAddEvent.setOnClickListener(v -> addEvent());
    }

    private void addEvent() {
        String title = editEventTitle.getText().toString().trim();
        String date = editEventDate.getText().toString().trim();

        // Validation: Title must be 3+ characters
        if (title.length() < 3) {
            showError(getString(R.string.title_min_chars));
            return;
        }

        // Validation: Date format YYYY-MM-DD
        if (!isValidDate(date)) {
            showError(getString(R.string.invalid_date_format));
            return;
        }

        hideError();

        // Add event
        Event newEvent = new Event(title, date);
        events.add(newEvent);
        Collections.sort(events, (e1, e2) -> e1.date.compareTo(e2.date));
        saveEvents();
        adapter.notifyDataSetChanged();
        updateEmptyState();

        // Clear inputs
        editEventTitle.setText("");
        editEventDate.setText("");

        Toast.makeText(this, R.string.event_added, Toast.LENGTH_SHORT).show();
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

    private void showError(String message) {
        textError.setText(message);
        textError.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        textError.setVisibility(View.GONE);
    }

    private void updateEmptyState() {
        if (events.isEmpty()) {
            textNoEvents.setVisibility(View.VISIBLE);
            recyclerEvents.setVisibility(View.GONE);
        } else {
            textNoEvents.setVisibility(View.GONE);
            recyclerEvents.setVisibility(View.VISIBLE);
        }
    }

    private void deleteEvent(int position) {
        events.remove(position);
        saveEvents();
        adapter.notifyItemRemoved(position);
        updateEmptyState();
        Toast.makeText(this, R.string.event_deleted, Toast.LENGTH_SHORT).show();
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

    // Event data class
    static class Event {
        String title;
        String date;

        Event(String title, String date) {
            this.title = title;
            this.date = date;
        }
    }

    // Event Adapter
    class EventAdapter extends RecyclerView.Adapter<EventAdapter.ViewHolder> {
        private final List<Event> items;
        private final String[] months = {"JAN", "FEB", "MAR", "APR", "MAY", "JUN",
                "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"};

        EventAdapter(List<Event> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_event, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Event event = items.get(position);
            holder.textEventTitle.setText(event.title);
            holder.textEventDate.setText(event.date);

            // Parse date for badge
            String[] parts = event.date.split("-");
            if (parts.length == 3) {
                int month = Integer.parseInt(parts[1]) - 1;
                if (month >= 0 && month < 12) {
                    holder.textMonth.setText(months[month]);
                }
                holder.textDay.setText(parts[2]);
            }

            holder.btnDelete.setOnClickListener(v -> deleteEvent(holder.getAdapterPosition()));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textMonth, textDay, textEventTitle, textEventDate;
            ImageButton btnDelete;

            ViewHolder(View itemView) {
                super(itemView);
                textMonth = itemView.findViewById(R.id.textMonth);
                textDay = itemView.findViewById(R.id.textDay);
                textEventTitle = itemView.findViewById(R.id.textEventTitle);
                textEventDate = itemView.findViewById(R.id.textEventDate);
                btnDelete = itemView.findViewById(R.id.btnDelete);
            }
        }
    }
}
