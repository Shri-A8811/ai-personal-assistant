package com.mitaoe.shridhar202401040197.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mitaoe.shridhar202401040197.R;
import com.mitaoe.shridhar202401040197.ai.AiService;
import com.mitaoe.shridhar202401040197.data.database.AppDatabase;
import com.mitaoe.shridhar202401040197.data.model.NoteItem;
import com.mitaoe.shridhar202401040197.data.model.TaskItem;
import com.mitaoe.shridhar202401040197.service.NotificationHelper;
import com.mitaoe.shridhar202401040197.ui.tasks.TaskAdapter;
import com.mitaoe.shridhar202401040197.ui.tasks.TaskDialog;
import com.mitaoe.shridhar202401040197.util.DateTimeUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class DashboardFragment extends Fragment {

    private TextView txtBriefingContent;
    private ProgressBar progressBriefing;
    private ImageView btnRefreshBriefing;
    private TextView txtMetricCompleted, txtMetricPending, txtMetricNotes;
    private TextView txtNoTasksToday;
    private RecyclerView recyclerDashboardTasks;
    private TaskAdapter taskAdapter;

    private AppDatabase db;
    private AiService aiService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        db = AppDatabase.getInstance(requireContext());
        aiService = new AiService(requireContext());

        txtBriefingContent = view.findViewById(R.id.txtBriefingContent);
        progressBriefing = view.findViewById(R.id.progressBriefing);
        btnRefreshBriefing = view.findViewById(R.id.btnRefreshBriefing);
        txtMetricCompleted = view.findViewById(R.id.txtMetricCompleted);
        txtMetricPending = view.findViewById(R.id.txtMetricPending);
        txtMetricNotes = view.findViewById(R.id.txtMetricNotes);
        txtNoTasksToday = view.findViewById(R.id.txtNoTasksToday);
        recyclerDashboardTasks = view.findViewById(R.id.recyclerDashboardTasks);

        recyclerDashboardTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        taskAdapter = new TaskAdapter(new TaskAdapter.TaskActionListener() {
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
            }

            @Override
            public void onEditTask(TaskItem task) {
                TaskDialog.newInstance(task).show(getParentFragmentManager(), "EditTaskDialog");
            }
        });
        recyclerDashboardTasks.setAdapter(taskAdapter);

        // Metrics Observation
        db.taskDao().getCompletedTasks().observe(getViewLifecycleOwner(), completed -> {
            txtMetricCompleted.setText(String.valueOf(completed != null ? completed.size() : 0));
        });

        db.taskDao().getPendingTasks().observe(getViewLifecycleOwner(), pending -> {
            txtMetricPending.setText(String.valueOf(pending != null ? pending.size() : 0));
        });

        db.noteDao().getAllNotes().observe(getViewLifecycleOwner(), notes -> {
            txtMetricNotes.setText(String.valueOf(notes != null ? notes.size() : 0));
        });

        // Tasks Due Today Observation
        db.taskDao().getTasksDueToday(DateTimeUtil.getEndOfTodayMillis()).observe(getViewLifecycleOwner(), tasks -> {
            if (tasks != null) {
                taskAdapter.setTasks(tasks);
                txtNoTasksToday.setVisibility(tasks.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });

        btnRefreshBriefing.setOnClickListener(v -> generateAiBriefing());

        generateAiBriefing();

        return view;
    }

    private void generateAiBriefing() {
        progressBriefing.setVisibility(View.VISIBLE);
        txtBriefingContent.setText("Preparing your personalized morning briefing...");

        Executors.newSingleThreadExecutor().execute(() -> {
            List<TaskItem> pending = db.taskDao().getPendingTasksSync();
            List<NoteItem> notes = db.noteDao().getAllNotesSync();

            StringBuilder contextSummary = new StringBuilder();
            contextSummary.append("Pending tasks count: ").append(pending.size()).append(".\n");
            for (int i = 0; i < Math.min(pending.size(), 5); i++) {
                TaskItem t = pending.get(i);
                contextSummary.append("- ").append(t.getTitle()).append(" (Priority: ").append(t.getPriority()).append(")\n");
            }
            contextSummary.append("Total active notes: ").append(notes.size()).append(".\n");

            String prompt = "You are a motivating, warm, and highly organized AI personal assistant. Generate a short 2-3 sentence morning briefing for the user based on these tasks:\n\n"
                    + contextSummary.toString()
                    + "\nHighlight any urgent priorities and wish them a productive day.";

            aiService.generateResponse(new ArrayList<>(), prompt, new AiService.StreamCallback() {
                @Override public void onStart() {}
                @Override public void onToken(String token, String fullTextSoFar) {}

                @Override
                public void onComplete(String fullResponse) {
                    if (isAdded()) {
                        progressBriefing.setVisibility(View.GONE);
                        txtBriefingContent.setText(fullResponse);
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    if (isAdded()) {
                        progressBriefing.setVisibility(View.GONE);
                        txtBriefingContent.setText("Good day! You have " + pending.size() + " pending tasks. Let's make today productive and check off your priorities!");
                    }
                }
            });
        });
    }
}
