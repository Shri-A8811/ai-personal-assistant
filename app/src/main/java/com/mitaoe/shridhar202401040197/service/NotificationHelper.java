package com.mitaoe.shridhar202401040197.service;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.mitaoe.shridhar202401040197.MainActivity;
import com.mitaoe.shridhar202401040197.R;
import com.mitaoe.shridhar202401040197.receiver.AlarmReceiver;
import com.mitaoe.shridhar202401040197.receiver.NotificationActionReceiver;

public class NotificationHelper {

    public static final String CHANNEL_ID = "ai_reminders_channel";
    public static final String CHANNEL_NAME = "Tasks & Reminders";

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("High priority notifications for scheduled tasks & reminders");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 200, 500});

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    public static void showReminderNotification(Context context, long taskId, String taskTitle, String taskDesc) {
        createNotificationChannel(context);

        Intent openAppIntent = new Intent(context, MainActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openAppPendingIntent = PendingIntent.getActivity(
                context, (int) taskId, openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Action: Complete
        Intent completeIntent = new Intent(context, NotificationActionReceiver.class);
        completeIntent.setAction(NotificationActionReceiver.ACTION_COMPLETE);
        completeIntent.putExtra(NotificationActionReceiver.EXTRA_TASK_ID, taskId);
        PendingIntent completePendingIntent = PendingIntent.getBroadcast(
                context, (int) (taskId * 10 + 1), completeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Action: Snooze 10 mins
        Intent snoozeIntent = new Intent(context, NotificationActionReceiver.class);
        snoozeIntent.setAction(NotificationActionReceiver.ACTION_SNOOZE);
        snoozeIntent.putExtra(NotificationActionReceiver.EXTRA_TASK_ID, taskId);
        snoozeIntent.putExtra(NotificationActionReceiver.EXTRA_TASK_TITLE, taskTitle);
        PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(
                context, (int) (taskId * 10 + 2), snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Uri defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_tasks)
                .setContentTitle(taskTitle != null ? taskTitle : "Task Reminder")
                .setContentText(taskDesc != null && !taskDesc.isEmpty() ? taskDesc : "Your scheduled reminder is due!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setSound(defaultSound)
                .setVibrate(new long[]{0, 500, 200, 500})
                .setAutoCancel(true)
                .setContentIntent(openAppPendingIntent)
                .addAction(R.drawable.ic_check, "Mark as Done", completePendingIntent)
                .addAction(R.drawable.ic_time, "Snooze 10m", snoozePendingIntent);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify((int) taskId, builder.build());
        }
    }

    public static void scheduleAlarm(Context context, long taskId, String taskTitle, long triggerMillis) {
        if (triggerMillis <= System.currentTimeMillis()) return;

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("task_id", taskId);
        intent.putExtra("task_title", taskTitle);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                (int) taskId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent);
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent);
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent);
            }
        } catch (SecurityException e) {
            // Fallback for devices where exact alarm permission is restricted
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent);
        }
    }

    public static void cancelAlarm(Context context, long taskId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                (int) taskId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        alarmManager.cancel(pendingIntent);
    }
}
