package com.example.fblaapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class ReminderBroadcastReceiver extends BroadcastReceiver {

    public static final String CHANNEL_ID = "fbla_event_reminders";
    public static final String CHANNEL_NAME = "Event Reminders";

    public static final String EXTRA_REMINDER_ID = "reminder_id";
    public static final String EXTRA_EVENT_ID = "event_id";
    public static final String EXTRA_EVENT_TITLE = "event_title";
    public static final String EXTRA_EVENT_START = "event_start";

    @Override
    public void onReceive(Context context, Intent intent) {
        long reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1);
        long eventId = intent.getLongExtra(EXTRA_EVENT_ID, -1);
        String eventTitle = intent.getStringExtra(EXTRA_EVENT_TITLE);
        long eventStart = intent.getLongExtra(EXTRA_EVENT_START, 0);

        if (eventTitle == null || eventTitle.isEmpty()) {
            eventTitle = "FBLA Event";
        }

        // Create notification channel for Android O+
        createNotificationChannel(context);

        // Create intent to open event detail
        Intent detailIntent = new Intent(context, EventDetailActivity.class);
        detailIntent.putExtra("event_id", eventId);
        detailIntent.putExtra("event_title", eventTitle);
        detailIntent.putExtra("event_start", eventStart);
        detailIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                (int) eventId,
                detailIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Event Reminder")
                .setContentText(eventTitle + " is coming up!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        // Show notification
        NotificationManager notificationManager = 
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        if (notificationManager != null) {
            notificationManager.notify((int) reminderId, builder.build());
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Reminders for FBLA events");
            channel.enableVibration(true);

            NotificationManager notificationManager = 
                    context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
