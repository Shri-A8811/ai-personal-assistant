package com.mitaoe.shridhar202401040197.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.mitaoe.shridhar202401040197.data.database.AppDatabase;
import com.mitaoe.shridhar202401040197.data.model.TaskItem;
import com.mitaoe.shridhar202401040197.service.NotificationHelper;
import com.mitaoe.shridhar202401040197.util.AppExecutors;

import java.util.List;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            AppExecutors.getInstance().diskIO().execute(() -> {
                AppDatabase db = AppDatabase.getInstance(context);
                List<TaskItem> pending = db.taskDao().getPendingTasksSync();
                long now = System.currentTimeMillis();
                for (TaskItem task : pending) {
                    if (task.isHasReminder()) {
                        long due = task.getDueDateMillis();
                        if (task.getRecurrence() != null && !task.getRecurrence().equalsIgnoreCase("NONE") && due <= now) {
                            long interval = "DAILY".equalsIgnoreCase(task.getRecurrence()) ? (24L * 60 * 60 * 1000) : (7L * 24 * 60 * 60 * 1000);
                            while (due <= now) {
                                due += interval;
                            }
                            task.setDueDateMillis(due);
                            db.taskDao().update(task);
                        }
                        if (due > now) {
                            NotificationHelper.scheduleAlarm(context, task.getId(), task.getTitle(), due);
                        }
                    }
                }
            });
        }
    }
}
