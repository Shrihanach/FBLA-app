package com.example.fblaapp.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.example.fblaapp.ReminderBroadcastReceiver;
import com.example.fblaapp.data.ReminderEntity;

/**
 * Utility class for scheduling and cancelling reminder alarms.
 */
public class ReminderScheduler {

    private final Context context;
    private final AlarmManager alarmManager;

    public ReminderScheduler(Context context) {
        this.context = context.getApplicationContext();
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    /**
     * Schedule a reminder notification.
     */
    public void scheduleReminder(ReminderEntity reminder, String eventTitle, long eventStart) {
        if (reminder == null || !reminder.isEnabled()) {
            return;
        }

        // Don't schedule if reminder time has already passed
        if (reminder.getRemindAtMillis() <= System.currentTimeMillis()) {
            return;
        }

        Intent intent = new Intent(context, ReminderBroadcastReceiver.class);
        intent.putExtra(ReminderBroadcastReceiver.EXTRA_REMINDER_ID, reminder.getId());
        intent.putExtra(ReminderBroadcastReceiver.EXTRA_EVENT_ID, reminder.getEventId());
        intent.putExtra(ReminderBroadcastReceiver.EXTRA_EVENT_TITLE, eventTitle);
        intent.putExtra(ReminderBroadcastReceiver.EXTRA_EVENT_START, eventStart);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                (int) reminder.getId(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        reminder.getRemindAtMillis(),
                        pendingIntent
                );
            } else {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        reminder.getRemindAtMillis(),
                        pendingIntent
                );
            }
        }
    }

    /**
     * Cancel a scheduled reminder.
     */
    public void cancelReminder(long reminderId) {
        Intent intent = new Intent(context, ReminderBroadcastReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                (int) reminderId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }

    /**
     * Calculate reminder time based on offset type.
     * @param eventStartMillis Event start time in milliseconds.
     * @param offsetType 0 = 1 hour before, 1 = 1 day before, 2 = custom (returns eventStart).
     * @return Reminder time in milliseconds.
     */
    public static long calculateReminderTime(long eventStartMillis, int offsetType) {
        switch (offsetType) {
            case 0: // 1 hour before
                return eventStartMillis - (60 * 60 * 1000L);
            case 1: // 1 day before
                return eventStartMillis - (24 * 60 * 60 * 1000L);
            case 2: // Custom (handled separately)
            default:
                return eventStartMillis;
        }
    }
}
