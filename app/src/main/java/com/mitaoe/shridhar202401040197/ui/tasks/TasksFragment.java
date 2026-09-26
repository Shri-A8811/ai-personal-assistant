package com.mitaoe.shridhar202401040197.ui.tasks;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.mitaoe.shridhar202401040197.R;
import com.mitaoe.shridhar202401040197.data.database.AppDatabase;
import com.mitaoe.shridhar202401040197.data.model.TaskItem;
import com.mitaoe.shridhar202401040197.service.NotificationHelper;

import java.util.List;
import java.util.concurrent.Executors;

public class TasksFragment extends Fragment {

    private RecyclerView recyclerTasks;
    private TaskAdapter adapter;
    private TextView txtEmptyTasks;
    private AppDatabase db;
    private LiveData<List<TaskItem>> currentLiveData;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tasks, container, false);

        db = AppDatabase.getInstance(requireContext());

        recyclerTasks = view.findViewById(R.id.recyclerTasks);
        txtEmptyTasks = view.findViewById(R.id.txtEmptyTasks);
        ChipGroup chipGroupFilters = view.findViewById(R.id.chipGroupFilters);
        ExtendedFloatingActionButton fabAddTask = view.findViewById(R.id.fabAddTask);

        recyclerTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new TaskAdapter(new TaskAdapter.TaskActionListener() {
            @Override
            public void onToggleComplete(TaskItem task, boolean isCompleted) {
                Executors.newSingleThreadExecutor().execute(() -> {
                    task.setCompleted(isCompleted);
                    db.taskDao().update(task);
                    if (isCompleted) {
                        NotificationHelper.cancelAlarm(requireContext(), task.getId());
                    }
                });
            }

            @Override
            public void onDeleteTask(TaskItem task) {
                Executors.newSingleThreadExecutor().execute(() -> {
                    db.taskDao().delete(task);
                    NotificationHelper.cancelAlarm(requireContext(), task.getId());
                });
                Toast.makeText(requireContext(), "Task deleted", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onEditTask(TaskItem task) {
                TaskDialog.newInstance(task).show(getParentFragmentManager(), "EditTaskDialog");
            }
        });
        recyclerTasks.setAdapter(adapter);

        fabAddTask.setOnClickListener(v -> {
            TaskDialog.newInstance(null).show(getParentFragmentManager(), "NewTaskDialog");
        });

        chipGroupFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chipPending)) {
                observeTasks(db.taskDao().getPendingTasks());
            } else if (checkedIds.contains(R.id.chipCompleted)) {
                observeTasks(db.taskDao().getCompletedTasks());
            } else {
                observeTasks(db.taskDao().getAllTasks());
            }
        });

        // Default: Observe all tasks
        observeTasks(db.taskDao().getAllTasks());

        return view;
    }

    private void observeTasks(LiveData<List<TaskItem>> liveData) {
        if (currentLiveData != null) {
            currentLiveData.removeObservers(getViewLifecycleOwner());
        }
        currentLiveData = liveData;
        currentLiveData.observe(getViewLifecycleOwner(), tasks -> {
            if (tasks != null) {
                adapter.setTasks(tasks);
                txtEmptyTasks.setVisibility(tasks.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });
    }
}
