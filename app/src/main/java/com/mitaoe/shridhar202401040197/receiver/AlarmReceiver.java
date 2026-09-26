package com.mitaoe.shridhar202401040197.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.mitaoe.shridhar202401040197.data.database.AppDatabase;
import com.mitaoe.shridhar202401040197.data.model.TaskItem;
import com.mitaoe.shridhar202401040197.service.NotificationHelper;

import java.util.concurrent.Executors;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        long taskId = intent.getLongExtra("task_id", -1);
        String taskTitle = intent.getStringExtra("task_title");

        if (taskId != -1) {
            Executors.newSingleThreadExecutor().execute(() -> {
                AppDatabase db = AppDatabase.getInstance(context);
                TaskItem task = db.taskDao().getTaskById(taskId);
                if (task != null && !task.isCompleted()) {
                    NotificationHelper.showReminderNotification(
                            context,
                            task.getId(),
                            task.getTitle(),
                            task.getDescription()
                    );
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
