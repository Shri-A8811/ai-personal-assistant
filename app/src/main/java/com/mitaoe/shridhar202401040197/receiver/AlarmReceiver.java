package com.mitaoe.shridhar202401040197.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.mitaoe.shridhar202401040197.data.database.AppDatabase;
import com.mitaoe.shridhar202401040197.data.model.TaskItem;
import com.mitaoe.shridhar202401040197.service.NotificationHelper;
import com.mitaoe.shridhar202401040197.util.AppExecutors;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        long taskId = intent.getLongExtra("task_id", -1);
        String taskTitle = intent.getStringExtra("task_title");

        if (taskId != -1) {
            AppExecutors.getInstance().diskIO().execute(() -> {
                AppDatabase db = AppDatabase.getInstance(context);
                TaskItem task = db.taskDao().getTaskById(taskId);
                if (task != null && !task.isCompleted()) {
                    NotificationHelper.showReminderNotification(
                            context,
                            task.getId(),
                            task.getTitle(),
                            task.getDescription()
                    );
                    if (task.getRecurrence() != null && !task.getRecurrence().equalsIgnoreCase("NONE")) {
                        long nextTrigger = task.getDueDateMillis();
                        long interval = "DAILY".equalsIgnoreCase(task.getRecurrence()) ? (24L * 60 * 60 * 1000) : (7L * 24 * 60 * 60 * 1000);
                        while (nextTrigger <= System.currentTimeMillis()) {
                            nextTrigger += interval;
                        }
                        task.setDueDateMillis(nextTrigger);
                        db.taskDao().update(task);
                        NotificationHelper.scheduleAlarm(context, task.getId(), task.getTitle(), nextTrigger);
                    }
                } else if (taskTitle != null) {
                    NotificationHelper.showReminderNotification(
                            context,
                            taskId,
                            taskTitle,
                            "Reminder Alert"
                    );
                }
            });
        }
    }
}
