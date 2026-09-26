package com.mitaoe.shridhar202401040197.ui.tasks;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.mitaoe.shridhar202401040197.R;
import com.mitaoe.shridhar202401040197.data.model.TaskItem;
import com.mitaoe.shridhar202401040197.util.DateTimeUtil;

import java.util.ArrayList;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    public interface TaskActionListener {
        void onToggleComplete(TaskItem task, boolean isCompleted);
        void onDeleteTask(TaskItem task);
        void onEditTask(TaskItem task);
    }

    private List<TaskItem> tasks = new ArrayList<>();
    private final TaskActionListener listener;

    public TaskAdapter(TaskActionListener listener) {
        this.listener = listener;
    }

    public void setTasks(List<TaskItem> tasks) {
        this.tasks = tasks != null ? tasks : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        TaskItem task = tasks.get(position);

        holder.txtTaskTitle.setText(task.getTitle());

        if (task.getDescription() != null && !task.getDescription().isEmpty()) {
            holder.txtTaskDescription.setVisibility(View.VISIBLE);
            holder.txtTaskDescription.setText(task.getDescription());
        } else {
            holder.txtTaskDescription.setVisibility(View.GONE);
        }

        // Strikethrough for completed
        if (task.isCompleted()) {
            holder.txtTaskTitle.setPaintFlags(holder.txtTaskTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.txtTaskTitle.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_muted));
        } else {
            holder.txtTaskTitle.setPaintFlags(holder.txtTaskTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.txtTaskTitle.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_primary));
        }

        holder.checkTaskCompleted.setOnCheckedChangeListener(null);
        holder.checkTaskCompleted.setChecked(task.isCompleted());
        holder.checkTaskCompleted.setOnCheckedChangeListener((btn, isChecked) -> {
            if (listener != null) {
                listener.onToggleComplete(task, isChecked);
            }
        });

        // Priority Color
        String priority = task.getPriority() != null ? task.getPriority().toUpperCase() : "MEDIUM";
        holder.badgePriority.setText(priority);
        int colorRes = R.color.priority_medium;
        if ("HIGH".equals(priority)) colorRes = R.color.priority_high;
        else if ("LOW".equals(priority)) colorRes = R.color.priority_low;
        holder.badgePriority.setBackgroundTintList(ContextCompat.getColorStateList(holder.itemView.getContext(), colorRes));

        // Due Date
        if (task.getDueDateMillis() > 0) {
            holder.txtTaskDue.setVisibility(View.VISIBLE);
            holder.txtTaskDue.setText(DateTimeUtil.formatDateTime(task.getDueDateMillis()));
        } else {
            holder.txtTaskDue.setVisibility(View.GONE);
        }

        // Reminder Icon
        holder.imgHasReminder.setVisibility(task.isHasReminder() ? View.VISIBLE : View.GONE);

        holder.btnDeleteTask.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteTask(task);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditTask(task);
            }
        });
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkTaskCompleted;
        TextView txtTaskTitle, txtTaskDescription, badgePriority, txtTaskDue;
        ImageView imgHasReminder, btnDeleteTask;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            checkTaskCompleted = itemView.findViewById(R.id.checkTaskCompleted);
            txtTaskTitle = itemView.findViewById(R.id.txtTaskTitle);
            txtTaskDescription = itemView.findViewById(R.id.txtTaskDescription);
            badgePriority = itemView.findViewById(R.id.badgePriority);
            txtTaskDue = itemView.findViewById(R.id.txtTaskDue);
            imgHasReminder = itemView.findViewById(R.id.imgHasReminder);
            btnDeleteTask = itemView.findViewById(R.id.btnDeleteTask);
        }
    }
}
