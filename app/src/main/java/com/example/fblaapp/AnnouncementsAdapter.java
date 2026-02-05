package com.example.fblaapp;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fblaapp.data.AnnouncementEntity;

import java.util.ArrayList;
import java.util.List;

public class AnnouncementsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_ANNOUNCEMENT = 0;
    private static final int VIEW_TYPE_SHOW_MORE = 1;

    private List<AnnouncementEntity> announcements = new ArrayList<>();
    private boolean isOfficer = false;
    private long currentUserId = -1;
    private boolean showMoreVisible = false;
    private OnAnnouncementClickListener listener;

    public interface OnAnnouncementClickListener {
        void onEditClick(AnnouncementEntity announcement);
        void onDeleteClick(AnnouncementEntity announcement);
        void onShowMoreClick();
    }

    public void setOnAnnouncementClickListener(OnAnnouncementClickListener listener) {
        this.listener = listener;
    }

    public void setAnnouncements(List<AnnouncementEntity> announcements) {
        this.announcements = announcements != null ? announcements : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOfficer(boolean isOfficer) {
        this.isOfficer = isOfficer;
        notifyDataSetChanged();
    }

    public void setCurrentUserId(long userId) {
        this.currentUserId = userId;
        notifyDataSetChanged();
    }

    public void setShowMoreVisible(boolean visible) {
        this.showMoreVisible = visible;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (showMoreVisible && position == announcements.size()) {
            return VIEW_TYPE_SHOW_MORE;
        }
        return VIEW_TYPE_ANNOUNCEMENT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_SHOW_MORE) {
            View view = inflater.inflate(R.layout.item_show_more, parent, false);
            return new ShowMoreViewHolder(view);
        }
        View view = inflater.inflate(R.layout.item_announcement, parent, false);
        return new AnnouncementViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof AnnouncementViewHolder) {
            AnnouncementEntity announcement = announcements.get(position);
            ((AnnouncementViewHolder) holder).bind(announcement);
        } else if (holder instanceof ShowMoreViewHolder) {
            ((ShowMoreViewHolder) holder).bind();
        }
    }

    @Override
    public int getItemCount() {
        int count = announcements.size();
        if (showMoreVisible) {
            count++; // Add one for "Show More" button
        }
        return count;
    }

    class AnnouncementViewHolder extends RecyclerView.ViewHolder {
        private final TextView textTitle;
        private final TextView textAuthor;
        private final TextView textTimestamp;
        private final TextView textContent;
        private final ImageButton btnEdit;
        private final ImageButton btnDelete;

        public AnnouncementViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.textTitle);
            textAuthor = itemView.findViewById(R.id.textAuthor);
            textTimestamp = itemView.findViewById(R.id.textTimestamp);
            textContent = itemView.findViewById(R.id.textContent);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }

        public void bind(AnnouncementEntity announcement) {
            textTitle.setText(announcement.getTitle());
            textContent.setText(announcement.getContent());

            // Display author name
            String authorName = announcement.getAuthorName();
            if (authorName != null && !authorName.isEmpty()) {
                textAuthor.setText("Posted by " + authorName);
            } else {
                textAuthor.setText("Posted by Unknown");
            }

            // Format timestamp
            CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                    announcement.getCreatedAtMillis(),
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE
            );
            textTimestamp.setText(timeAgo);

            // Check if current user is the creator (can edit)
            boolean isCreator = announcement.getCreatedByUserId() == currentUserId;

            // Edit button (only for the officer who created it)
            if (isOfficer && isCreator) {
                btnEdit.setVisibility(View.VISIBLE);
                btnEdit.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onEditClick(announcement);
                    }
                });
            } else {
                btnEdit.setVisibility(View.GONE);
            }

            // Delete button (officers only)
            if (isOfficer) {
                btnDelete.setVisibility(View.VISIBLE);
                btnDelete.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onDeleteClick(announcement);
                    }
                });
            } else {
                btnDelete.setVisibility(View.GONE);
            }
        }
    }

    class ShowMoreViewHolder extends RecyclerView.ViewHolder {
        private final Button btnShowMore;

        public ShowMoreViewHolder(@NonNull View itemView) {
            super(itemView);
            btnShowMore = itemView.findViewById(R.id.btnShowMore);
        }

        public void bind() {
            btnShowMore.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onShowMoreClick();
                }
            });
        }
    }
}
