package com.mitaoe.shridhar202401040197.util;

import android.content.Context;
import android.widget.Toast;

import com.mitaoe.shridhar202401040197.data.database.AppDatabase;
import com.mitaoe.shridhar202401040197.data.model.TaskItem;
import com.mitaoe.shridhar202401040197.service.NotificationHelper;

public class TaskUtil {

    /**
     * Computes the next future trigger timestamp for a recurring task.
     * Avoids infinite or multi-decade catch-up loops when currentDueMillis <= 0.
     */
    public static long computeNextRecurrenceTrigger(long currentDueMillis, String recurrence, long nowMillis) {
        if (recurrence == null || "NONE".equalsIgnoreCase(recurrence.trim()) || recurrence.trim().isEmpty()) {
            return currentDueMillis;
        }
        long interval = "DAILY".equalsIgnoreCase(recurrence.trim())
                ? (24L * 60 * 60 * 1000)
                : (7L * 24 * 60 * 60 * 1000);
        if (currentDueMillis <= 0) {
            return nowMillis + interval;
        }
        long nextTrigger = currentDueMillis + interval;
        while (nextTrigger <= nowMillis) {
            nextTrigger += interval;
        }
        return nextTrigger;
    }

    public static void toggleTaskCompletion(Context context, TaskItem task, boolean isCompleted) {
        if (context == null || task == null) return;
        Context appContext = context.getApplicationContext();

        AppExecutors.getInstance().diskIO().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(appContext);
            if (isCompleted && task.getRecurrence() != null && !task.getRecurrence().equalsIgnoreCase("NONE")) {
                long nextTrigger = computeNextRecurrenceTrigger(
                        task.getDueDateMillis(),
                        task.getRecurrence(),
                        System.currentTimeMillis()
                );
                task.setDueDateMillis(nextTrigger);
                task.setCompleted(false);
                db.taskDao().update(task);
                if (task.isHasReminder()) {
                    NotificationHelper.scheduleAlarm(appContext, task.getId(), task.getTitle(), nextTrigger);
                }
                AppExecutors.getInstance().mainThread().execute(() -> {
                    Toast.makeText(appContext, "Recurring task completed! Scheduled for next cycle.", Toast.LENGTH_SHORT).show();
                });
            } else {
                task.setCompleted(isCompleted);
                db.taskDao().update(task);
                if (isCompleted) {
                    NotificationHelper.cancelAlarm(appContext, task.getId());
                }
            }
        });
    }
}
