package com.mitaoe.shridhar202401040197.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.mitaoe.shridhar202401040197.data.database.AppDatabase;
import com.mitaoe.shridhar202401040197.data.model.TaskItem;
import com.mitaoe.shridhar202401040197.service.NotificationHelper;
import com.mitaoe.shridhar202401040197.util.AppExecutors;
import com.mitaoe.shridhar202401040197.util.TaskUtil;

import java.util.List;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            PendingResult pendingResult = goAsync();
            AppExecutors.getInstance().diskIO().execute(() -> {
                try {
                    AppDatabase db = AppDatabase.getInstance(context);
                    List<TaskItem> pending = db.taskDao().getPendingTasksSync();
                    long now = System.currentTimeMillis();
                    for (TaskItem task : pending) {
                        if (task.isHasReminder()) {
                            long due = task.getDueDateMillis();
                            if (task.getRecurrence() != null && !task.getRecurrence().equalsIgnoreCase("NONE") && due <= now) {
                                due = TaskUtil.computeNextRecurrenceTrigger(due, task.getRecurrence(), now);
                                task.setDueDateMillis(due);
                                db.taskDao().update(task);
                            }
                            if (due > now) {
                                NotificationHelper.scheduleAlarm(context, task.getId(), task.getTitle(), due);
                            }
                        }
                    }
                } finally {
                    pendingResult.finish();
                }
            });
        }
    }
}
