package com.mitaoe.shridhar202401040197.receiver;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.mitaoe.shridhar202401040197.data.database.AppDatabase;
import com.mitaoe.shridhar202401040197.data.model.TaskItem;
import com.mitaoe.shridhar202401040197.service.NotificationHelper;
import com.mitaoe.shridhar202401040197.util.AppExecutors;

public class NotificationActionReceiver extends BroadcastReceiver {

    public static final String ACTION_COMPLETE = "com.mitaoe.shridhar202401040197.ACTION_COMPLETE";
    public static final String ACTION_SNOOZE = "com.mitaoe.shridhar202401040197.ACTION_SNOOZE";
    public static final String EXTRA_TASK_ID = "extra_task_id";
    public static final String EXTRA_TASK_TITLE = "extra_task_title";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        long taskId = intent.getLongExtra(EXTRA_TASK_ID, -1);
        String taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE);

        if (taskId == -1) return;

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.cancel((int) taskId);
        }

        if (ACTION_COMPLETE.equals(action)) {
            AppExecutors.getInstance().diskIO().execute(() -> {
                AppDatabase db = AppDatabase.getInstance(context);
                TaskItem task = db.taskDao().getTaskById(taskId);
                if (task != null && task.getRecurrence() != null && !task.getRecurrence().equalsIgnoreCase("NONE")) {
                    long nextTrigger = task.getDueDateMillis();
                    long interval = "DAILY".equalsIgnoreCase(task.getRecurrence()) ? (24L * 60 * 60 * 1000) : (7L * 24 * 60 * 60 * 1000);
                    while (nextTrigger <= System.currentTimeMillis()) {
                        nextTrigger += interval;
                    }
                    task.setDueDateMillis(nextTrigger);
                    task.setCompleted(false);
                    db.taskDao().update(task);
                    if (task.isHasReminder()) {
                        NotificationHelper.scheduleAlarm(context, task.getId(), task.getTitle(), nextTrigger);
                    }
                } else {
                    db.taskDao().setTaskCompleted(taskId, true);
                }
            });
            Toast.makeText(context, "Task completed! 🎉", Toast.LENGTH_SHORT).show();
        } else if (ACTION_SNOOZE.equals(action)) {
            long snoozeUntil = System.currentTimeMillis() + (10 * 60 * 1000); // +10 minutes
            NotificationHelper.scheduleAlarm(context, taskId, taskTitle, snoozeUntil);
            Toast.makeText(context, "Reminder snoozed for 10 minutes ⏰", Toast.LENGTH_SHORT).show();
        }
    }
}
