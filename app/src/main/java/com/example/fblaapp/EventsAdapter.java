package com.example.fblaapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fblaapp.data.EventEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.EventViewHolder> {

    private List<EventEntity> events = new ArrayList<>();
    private boolean isOfficer = false;
    private OnEventClickListener listener;

    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.US);
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.US);

    public interface OnEventClickListener {
        void onEventClick(EventEntity event);
        void onEditClick(EventEntity event);
        void onDeleteClick(EventEntity event);
    }

    public void setOnEventClickListener(OnEventClickListener listener) {
        this.listener = listener;
    }

    public void setEvents(List<EventEntity> events) {
        this.events = events != null ? events : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOfficer(boolean isOfficer) {
        this.isOfficer = isOfficer;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        EventEntity event = events.get(position);
        holder.bind(event);
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    class EventViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitle;
        private final TextView tvTime;
        private final TextView tvLocation;
        private final LinearLayout layoutLocation;
        private final LinearLayout layoutActions;
        private final ImageButton btnEdit;
        private final ImageButton btnDelete;
        private final View viewColorIndicator;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            layoutLocation = itemView.findViewById(R.id.layoutLocation);
            layoutActions = itemView.findViewById(R.id.layoutActions);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            viewColorIndicator = itemView.findViewById(R.id.viewColorIndicator);
        }

        public void bind(EventEntity event) {
            tvTitle.setText(event.getTitle());

            // Format date/time
            String startDateTime = dateTimeFormat.format(new Date(event.getStartTimeMillis()));
            String endTime = timeFormat.format(new Date(event.getEndTimeMillis()));
            tvTime.setText(startDateTime + " - " + endTime);

            // Location
            String location = event.getLocation();
            if (location != null && !location.trim().isEmpty()) {
                layoutLocation.setVisibility(View.VISIBLE);
                tvLocation.setText(location);
            } else {
                layoutLocation.setVisibility(View.GONE);
            }

            // Officer actions visibility
            if (isOfficer) {
                layoutActions.setVisibility(View.VISIBLE);
                btnEdit.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onEditClick(event);
                    }
                });
                btnDelete.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onDeleteClick(event);
                    }
                });
            } else {
                layoutActions.setVisibility(View.GONE);
            }

            // Row click
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEventClick(event);
                }
            });
        }
    }
}
