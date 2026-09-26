package com.mitaoe.shridhar202401040197.ui.tasks;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.mitaoe.shridhar202401040197.R;
import com.mitaoe.shridhar202401040197.data.database.AppDatabase;
import com.mitaoe.shridhar202401040197.data.model.TaskItem;
import com.mitaoe.shridhar202401040197.service.NotificationHelper;
import com.mitaoe.shridhar202401040197.util.DateTimeUtil;

import java.util.Calendar;
import java.util.concurrent.Executors;

public class TaskDialog extends DialogFragment {

    private TaskItem existingTask = null;
    private final Calendar selectedCal = Calendar.getInstance();
    private boolean isDateTimeChosen = false;

    public static TaskDialog newInstance(@Nullable TaskItem task) {
        TaskDialog dialog = new TaskDialog();
        dialog.existingTask = task;
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Context context = requireContext();
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_task_edit, null);

        TextView txtDialogTitle = view.findViewById(R.id.txtDialogTitle);
        EditText editTaskTitle = view.findViewById(R.id.editTaskTitle);
        EditText editTaskDesc = view.findViewById(R.id.editTaskDesc);
        Button btnPickDate = view.findViewById(R.id.btnPickDate);
        Button btnPickTime = view.findViewById(R.id.btnPickTime);
        RadioGroup radioGroupPriority = view.findViewById(R.id.radioGroupPriority);
        MaterialSwitch switchReminderAlarm = view.findViewById(R.id.switchReminderAlarm);
        Button btnCancel = view.findViewById(R.id.btnCancelTask);
        Button btnSave = view.findViewById(R.id.btnSaveTask);

        if (existingTask != null) {
            txtDialogTitle.setText("Edit Task");
            editTaskTitle.setText(existingTask.getTitle());
            editTaskDesc.setText(existingTask.getDescription());
            switchReminderAlarm.setChecked(existingTask.isHasReminder());

            if (existingTask.getDueDateMillis() > 0) {
                selectedCal.setTimeInMillis(existingTask.getDueDateMillis());
                isDateTimeChosen = true;
                btnPickDate.setText(DateTimeUtil.formatDate(existingTask.getDueDateMillis()));
                btnPickTime.setText(DateTimeUtil.formatTime(existingTask.getDueDateMillis()));
            }

            if ("HIGH".equalsIgnoreCase(existingTask.getPriority())) {
                radioGroupPriority.check(R.id.radioPriorityHigh);
            } else if ("LOW".equalsIgnoreCase(existingTask.getPriority())) {
                radioGroupPriority.check(R.id.radioPriorityLow);
            } else {
                radioGroupPriority.check(R.id.radioPriorityMed);
            }
        } else {
            // Default 1 hour from now
            selectedCal.add(Calendar.HOUR_OF_DAY, 1);
            btnPickDate.setText(DateTimeUtil.formatDate(selectedCal.getTimeInMillis()));
            btnPickTime.setText(DateTimeUtil.formatTime(selectedCal.getTimeInMillis()));
            isDateTimeChosen = true;
        }

        btnPickDate.setOnClickListener(v -> {
            new DatePickerDialog(context, (picker, year, month, day) -> {
                selectedCal.set(Calendar.YEAR, year);
                selectedCal.set(Calendar.MONTH, month);
                selectedCal.set(Calendar.DAY_OF_MONTH, day);
                isDateTimeChosen = true;
                btnPickDate.setText(DateTimeUtil.formatDate(selectedCal.getTimeInMillis()));
            }, selectedCal.get(Calendar.YEAR), selectedCal.get(Calendar.MONTH), selectedCal.get(Calendar.DAY_OF_MONTH)).show();
        });

        btnPickTime.setOnClickListener(v -> {
            new TimePickerDialog(context, (picker, hour, minute) -> {
                selectedCal.set(Calendar.HOUR_OF_DAY, hour);
                selectedCal.set(Calendar.MINUTE, minute);
                selectedCal.set(Calendar.SECOND, 0);
                isDateTimeChosen = true;
                btnPickTime.setText(DateTimeUtil.formatTime(selectedCal.getTimeInMillis()));
            }, selectedCal.get(Calendar.HOUR_OF_DAY), selectedCal.get(Calendar.MINUTE), false).show();
        });

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(view)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnCancel.setOnClickListener(v -> dismiss());

        btnSave.setOnClickListener(v -> {
            String title = editTaskTitle.getText().toString().trim();
            if (title.isEmpty()) {
                Toast.makeText(context, "Please enter a task title", Toast.LENGTH_SHORT).show();
                return;
            }

            String desc = editTaskDesc.getText().toString().trim();
            String priority = "MEDIUM";
            int checkedId = radioGroupPriority.getCheckedRadioButtonId();
            if (checkedId == R.id.radioPriorityHigh) priority = "HIGH";
            else if (checkedId == R.id.radioPriorityLow) priority = "LOW";

            boolean hasReminder = switchReminderAlarm.isChecked();
            long dueMillis = isDateTimeChosen ? selectedCal.getTimeInMillis() : 0;

            AppDatabase db = AppDatabase.getInstance(context);

            if (existingTask == null) {
                TaskItem newTask = new TaskItem(title, desc, dueMillis, priority, "Tasks", hasReminder);
                Executors.newSingleThreadExecutor().execute(() -> {
                    long id = db.taskDao().insert(newTask);
                    if (hasReminder && dueMillis > System.currentTimeMillis()) {
                        NotificationHelper.scheduleAlarm(context, id, title, dueMillis);
                    }
                });
                Toast.makeText(context, "Task created!", Toast.LENGTH_SHORT).show();
            } else {
                existingTask.setTitle(title);
                existingTask.setDescription(desc);
                existingTask.setDueDateMillis(dueMillis);
                existingTask.setPriority(priority);
                existingTask.setHasReminder(hasReminder);

                Executors.newSingleThreadExecutor().execute(() -> {
                    db.taskDao().update(existingTask);
                    if (hasReminder && dueMillis > System.currentTimeMillis()) {
                        NotificationHelper.scheduleAlarm(context, existingTask.getId(), title, dueMillis);
                    } else {
                        NotificationHelper.cancelAlarm(context, existingTask.getId());
                    }
                });
                Toast.makeText(context, "Task updated!", Toast.LENGTH_SHORT).show();
            }

            dismiss();
        });

        return dialog;
    }
}
