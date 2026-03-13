package com.example.fblaapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.text.Html;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fblaapp.data.AnnouncementEntity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AnnouncementsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_ANNOUNCEMENT = 0;
    private static final int VIEW_TYPE_SHOW_MORE = 1;
    private static final String POLL_PREFS = "FBLAPollVotes";

    private List<AnnouncementEntity> announcements = new ArrayList<>();
    private boolean isOfficer = false;
    private long currentUserId = -1;
    private boolean showMoreVisible = false;
    private OnAnnouncementClickListener listener;

    public interface OnAnnouncementClickListener {
        void onEditClick(AnnouncementEntity announcement, View itemView);
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
        if (showMoreVisible) count++;
        return count;
    }

    // ==================== Announcement ViewHolder ====================

    class AnnouncementViewHolder extends RecyclerView.ViewHolder {
        private final TextView textTitle;
        private final TextView textAuthor;
        private final TextView textTimestamp;
        private final TextView textContent;
        private final ImageButton btnEdit;
        private final ImageButton btnDelete;
        private final LinearLayout layoutAttachments;
        private final LinearLayout layoutPoll;
        private final TextView textPollQuestion;
        private final LinearLayout layoutPollOptions;

        public AnnouncementViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.textTitle);
            textAuthor = itemView.findViewById(R.id.textAuthor);
            textTimestamp = itemView.findViewById(R.id.textTimestamp);
            textContent = itemView.findViewById(R.id.textContent);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            layoutAttachments = itemView.findViewById(R.id.layoutAttachments);
            layoutPoll = itemView.findViewById(R.id.layoutPoll);
            textPollQuestion = itemView.findViewById(R.id.textPollQuestion);
            layoutPollOptions = itemView.findViewById(R.id.layoutPollOptions);
        }

        public void bind(AnnouncementEntity announcement) {
            Context ctx = itemView.getContext();

            textTitle.setText(announcement.getTitle());

            // Render HTML content (preserves bold, underline, etc.)
            String content = announcement.getContent();
            if (content != null && (content.contains("<") && content.contains(">"))) {
                textContent.setText(Html.fromHtml(content, Html.FROM_HTML_MODE_COMPACT));
                textContent.setMovementMethod(LinkMovementMethod.getInstance());
            } else {
                textContent.setText(content);
            }

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

            // Edit/Delete buttons
            boolean isCreator = announcement.getCreatedByUserId() == currentUserId;
            if (isOfficer && isCreator) {
                btnEdit.setVisibility(View.VISIBLE);
                btnEdit.setOnClickListener(v -> {
                    if (listener != null) listener.onEditClick(announcement, itemView);
                });
            } else {
                btnEdit.setVisibility(View.GONE);
            }

            if (isOfficer) {
                btnDelete.setVisibility(View.VISIBLE);
                btnDelete.setOnClickListener(v -> {
                    if (listener != null) listener.onDeleteClick(announcement);
                });
            } else {
                btnDelete.setVisibility(View.GONE);
            }

            // ── Attachments ──
            bindAttachments(announcement, ctx);
        }

        private void bindAttachments(AnnouncementEntity announcement, Context ctx) {
            layoutAttachments.removeAllViews();
            layoutPoll.setVisibility(View.GONE);
            layoutAttachments.setVisibility(View.GONE);

            String attachJson = announcement.getAttachmentJson();
            if (attachJson == null || attachJson.isEmpty()) return;

            JSONObject attachments;
            try {
                attachments = new JSONObject(attachJson);
            } catch (JSONException e) {
                return;
            }

            boolean hasItems = false;

            // ── Files ──
            JSONArray files = attachments.optJSONArray("files");
            if (files != null && files.length() > 0) {
                hasItems = true;
                for (int i = 0; i < files.length(); i++) {
                    JSONObject f = files.optJSONObject(i);
                    if (f == null) continue;
                    String name = f.optString("name", "File");
                    String path = f.optString("path", "");
                    layoutAttachments.addView(createAttachmentRow(ctx, "📎", name, v -> {
                        openFile(ctx, path);
                    }));
                }
            }

            // ── Links ──
            JSONArray links = attachments.optJSONArray("links");
            if (links != null && links.length() > 0) {
                hasItems = true;
                for (int i = 0; i < links.length(); i++) {
                    JSONObject l = links.optJSONObject(i);
                    if (l == null) continue;
                    String title = l.optString("title", "Link");
                    String url = l.optString("url", "");
                    layoutAttachments.addView(createAttachmentRow(ctx, "🔗", title, v -> {
                        try {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                            ctx.startActivity(intent);
                        } catch (Exception ignored) {}
                    }));
                }
            }

            // ── Audios ──
            JSONArray audios = attachments.optJSONArray("audios");
            if (audios != null && audios.length() > 0) {
                hasItems = true;
                for (int i = 0; i < audios.length(); i++) {
                    JSONObject a = audios.optJSONObject(i);
                    if (a == null) continue;
                    String name = a.optString("name", "Audio");
                    String path = a.optString("path", "");
                    layoutAttachments.addView(createAudioRow(ctx, name, path));
                }
            }

            if (hasItems) {
                layoutAttachments.setVisibility(View.VISIBLE);
            }

            // ── Poll ──
            JSONObject poll = attachments.optJSONObject("poll");
            if (poll != null) {
                bindPoll(announcement.getId(), poll, ctx);
            }
        }

        private View createAttachmentRow(Context ctx, String icon, String label,
                                          View.OnClickListener onClick) {
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(16, 12, 16, 12);
            row.setBackground(ContextCompat.getDrawable(ctx, R.drawable.poll_option_background));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.bottomMargin = 6;
            row.setLayoutParams(params);
            row.setClickable(true);
            row.setFocusable(true);
            row.setOnClickListener(onClick);

            TextView tvIcon = new TextView(ctx);
            tvIcon.setText(icon);
            tvIcon.setTextSize(16);
            tvIcon.setPadding(0, 0, 12, 0);
            row.addView(tvIcon);

            TextView tvLabel = new TextView(ctx);
            tvLabel.setText(label);
            tvLabel.setTextSize(13);
            tvLabel.setTextColor(Color.WHITE);
            tvLabel.setMaxLines(1);
            tvLabel.setEllipsize(android.text.TextUtils.TruncateAt.END);
            tvLabel.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(tvLabel);

            TextView tvOpen = new TextView(ctx);
            tvOpen.setText("Open ›");
            tvOpen.setTextSize(12);
            tvOpen.setTextColor(ContextCompat.getColor(ctx, R.color.fbla_gold));
            row.addView(tvOpen);

            return row;
        }

        private View createAudioRow(Context ctx, String name, String path) {
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(16, 12, 16, 12);
            row.setBackground(ContextCompat.getDrawable(ctx, R.drawable.poll_option_background));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.bottomMargin = 6;
            row.setLayoutParams(params);

            TextView tvIcon = new TextView(ctx);
            tvIcon.setText("🎤");
            tvIcon.setTextSize(16);
            tvIcon.setPadding(0, 0, 12, 0);
            row.addView(tvIcon);

            TextView tvLabel = new TextView(ctx);
            tvLabel.setText(name);
            tvLabel.setTextSize(13);
            tvLabel.setTextColor(Color.WHITE);
            tvLabel.setMaxLines(1);
            tvLabel.setEllipsize(android.text.TextUtils.TruncateAt.END);
            tvLabel.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(tvLabel);

            // Play button
            TextView btnPlay = new TextView(ctx);
            btnPlay.setText("▶ Play");
            btnPlay.setTextSize(12);
            btnPlay.setTextColor(ContextCompat.getColor(ctx, R.color.fbla_gold));
            btnPlay.setPadding(16, 0, 0, 0);
            btnPlay.setOnClickListener(v -> {
                try {
                    MediaPlayer mp = new MediaPlayer();
                    mp.setDataSource(path);
                    mp.prepare();
                    mp.start();
                    btnPlay.setText("⏹ Stop");
                    mp.setOnCompletionListener(m -> {
                        btnPlay.setText("▶ Play");
                        m.release();
                    });
                    btnPlay.setOnClickListener(v2 -> {
                        mp.stop();
                        mp.release();
                        btnPlay.setText("▶ Play");
                    });
                } catch (IOException e) {
                    Toast.makeText(ctx, "Cannot play audio", Toast.LENGTH_SHORT).show();
                }
            });
            row.addView(btnPlay);

            return row;
        }

        private void bindPoll(long announcementId, JSONObject poll, Context ctx) {
            layoutPoll.setVisibility(View.VISIBLE);
            layoutPollOptions.removeAllViews();

            String question = poll.optString("question", "");
            textPollQuestion.setText("📊 " + question);

            JSONArray options = poll.optJSONArray("options");
            if (options == null || options.length() == 0) return;

            SharedPreferences prefs = ctx.getSharedPreferences(POLL_PREFS, Context.MODE_PRIVATE);
            String voteKey = "vote_" + announcementId;
            int votedIndex = prefs.getInt(voteKey, -1);

            // Count votes from prefs (simulated local votes)
            String countKey = "counts_" + announcementId;
            int[] voteCounts = new int[options.length()];
            String countsStr = prefs.getString(countKey, null);
            int totalVotes = 0;
            if (countsStr != null) {
                String[] parts = countsStr.split(",");
                for (int i = 0; i < parts.length && i < voteCounts.length; i++) {
                    try { voteCounts[i] = Integer.parseInt(parts[i]); } catch (NumberFormatException ignored) {}
                    totalVotes += voteCounts[i];
                }
            }

            for (int i = 0; i < options.length(); i++) {
                String optionText = options.optString(i, "");
                final int optionIndex = i;
                final int finalTotal = totalVotes;

                LinearLayout optionRow = new LinearLayout(ctx);
                optionRow.setOrientation(LinearLayout.HORIZONTAL);
                optionRow.setGravity(Gravity.CENTER_VERTICAL);
                optionRow.setPadding(20, 16, 20, 16);
                optionRow.setBackground(ContextCompat.getDrawable(ctx,
                        votedIndex == i ? R.drawable.poll_option_selected : R.drawable.poll_option_background));
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                rowParams.bottomMargin = 6;
                optionRow.setLayoutParams(rowParams);

                // Option text
                TextView tvOption = new TextView(ctx);
                tvOption.setText(optionText);
                tvOption.setTextSize(13);
                tvOption.setTextColor(Color.WHITE);
                tvOption.setLayoutParams(new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                optionRow.addView(tvOption);

                // Show vote count if voted
                if (votedIndex >= 0 && finalTotal > 0) {
                    int pct = (int) ((voteCounts[i] * 100.0) / finalTotal);
                    TextView tvPct = new TextView(ctx);
                    tvPct.setText(voteCounts[i] + " (" + pct + "%)");
                    tvPct.setTextSize(12);
                    tvPct.setTextColor(ContextCompat.getColor(ctx, R.color.fbla_gold));
                    optionRow.addView(tvPct);
                }

                optionRow.setClickable(true);
                optionRow.setFocusable(true);
                optionRow.setOnClickListener(v -> {
                    if (votedIndex >= 0) {
                        Toast.makeText(ctx, "You already voted!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // Record vote
                    prefs.edit().putInt(voteKey, optionIndex).apply();
                    // Update counts
                    int[] newCounts = new int[options.length()];
                    String existingCounts = prefs.getString(countKey, null);
                    if (existingCounts != null) {
                        String[] parts = existingCounts.split(",");
                        for (int j = 0; j < parts.length && j < newCounts.length; j++) {
                            try { newCounts[j] = Integer.parseInt(parts[j]); } catch (NumberFormatException ignored) {}
                        }
                    }
                    newCounts[optionIndex]++;
                    StringBuilder sb = new StringBuilder();
                    for (int j = 0; j < newCounts.length; j++) {
                        if (j > 0) sb.append(",");
                        sb.append(newCounts[j]);
                    }
                    prefs.edit().putString(countKey, sb.toString()).apply();

                    // Refresh
                    notifyDataSetChanged();
                    Toast.makeText(ctx, "Vote recorded!", Toast.LENGTH_SHORT).show();
                });

                layoutPollOptions.addView(optionRow);
            }
        }

        private void openFile(Context ctx, String path) {
            try {
                File file = new File(path);
                if (!file.exists()) {
                    Toast.makeText(ctx, "File not found", Toast.LENGTH_SHORT).show();
                    return;
                }
                Uri uri = FileProvider.getUriForFile(ctx,
                        ctx.getPackageName() + ".fileprovider", file);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, ctx.getContentResolver().getType(uri));
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                ctx.startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(ctx, "Cannot open file", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ==================== Show More ViewHolder ====================

    class ShowMoreViewHolder extends RecyclerView.ViewHolder {
        private final Button btnShowMore;

        public ShowMoreViewHolder(@NonNull View itemView) {
            super(itemView);
            btnShowMore = itemView.findViewById(R.id.btnShowMore);
        }

        public void bind() {
            btnShowMore.setOnClickListener(v -> {
                if (listener != null) listener.onShowMoreClick();
            });
        }
    }
}
