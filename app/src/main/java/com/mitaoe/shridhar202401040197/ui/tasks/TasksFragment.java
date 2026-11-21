package com.mitaoe.shridhar202401040197.ui.tasks;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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
import com.mitaoe.shridhar202401040197.util.AppExecutors;

import java.util.List;

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
                Context ctx = getContext();
                if (ctx != null) {
                    com.mitaoe.shridhar202401040197.util.TaskUtil.toggleTaskCompletion(ctx.getApplicationContext(), task, isCompleted);
                }
            }

            @Override
            public void onDeleteTask(TaskItem task) {
                Context ctx = getContext();
                if (ctx == null) return;
                final Context appContext = ctx.getApplicationContext();
                new AlertDialog.Builder(ctx)
                        .setTitle("Delete Task")
                        .setMessage("Delete \"" + (task.getTitle() != null ? task.getTitle() : "task") + "\"?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            AppExecutors.getInstance().diskIO().execute(() -> {
                                db.taskDao().delete(task);
                                NotificationHelper.cancelAlarm(appContext, task.getId());
                            });
                            Toast.makeText(appContext, "Task deleted", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }

            @Override
            public void onEditTask(TaskItem task) {
                if (!isAdded() || getParentFragmentManager().isStateSaved()) return;
                TaskDialog.newInstance(task).show(getParentFragmentManager(), "EditTaskDialog");
            }
        });
        recyclerTasks.setAdapter(adapter);

        fabAddTask.setOnClickListener(v -> {
            if (!isAdded() || getParentFragmentManager().isStateSaved()) return;
            TaskDialog.newInstance(null).show(getParentFragmentManager(), "NewTaskDialog");
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ChipGroup chipGroupFilters = view.findViewById(R.id.chipGroupFilters);
        if (chipGroupFilters != null) {
            chipGroupFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (checkedIds.contains(R.id.chipPending)) {
                    observeTasks(db.taskDao().getPendingTasks());
                } else if (checkedIds.contains(R.id.chipCompleted)) {
                    observeTasks(db.taskDao().getCompletedTasks());
                } else {
                    observeTasks(db.taskDao().getAllTasks());
                }
            });
        }

        // Default: Observe all tasks
        observeTasks(db.taskDao().getAllTasks());
    }

    private void observeTasks(LiveData<List<TaskItem>> liveData) {
        if (!isAdded() || getView() == null) return;
        if (currentLiveData != null) {
            try {
                currentLiveData.removeObservers(getViewLifecycleOwner());
            } catch (Exception ignored) {}
        }
        currentLiveData = liveData;
        try {
            currentLiveData.observe(getViewLifecycleOwner(), tasks -> {
                if (!isAdded() || getView() == null) return;
                if (tasks != null) {
                    if (adapter != null) adapter.setTasks(tasks);
                    if (txtEmptyTasks != null) {
                        txtEmptyTasks.setVisibility(tasks.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                }
            });
        } catch (Exception ignored) {}
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (currentLiveData != null) {
            try {
                currentLiveData.removeObservers(getViewLifecycleOwner());
            } catch (Exception ignored) {}
            currentLiveData = null;
        }
    }
}
