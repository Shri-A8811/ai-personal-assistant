package com.mitaoe.shridhar202401040197.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.mitaoe.shridhar202401040197.data.database.AppDatabase;
import com.mitaoe.shridhar202401040197.data.model.TaskItem;
import com.mitaoe.shridhar202401040197.service.NotificationHelper;

import java.util.List;
import java.util.concurrent.Executors;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Executors.newSingleThreadExecutor().execute(() -> {
                AppDatabase db = AppDatabase.getInstance(context);
                List<TaskItem> pending = db.taskDao().getPendingTasksSync();
                long now = System.currentTimeMillis();
                for (TaskItem task : pending) {
                    if (task.isHasReminder() && task.getDueDateMillis() > now) {
                        NotificationHelper.scheduleAlarm(context, task.getId(), task.getTitle(), task.getDueDateMillis());
                    }
                }
            });
        }
    }
}
