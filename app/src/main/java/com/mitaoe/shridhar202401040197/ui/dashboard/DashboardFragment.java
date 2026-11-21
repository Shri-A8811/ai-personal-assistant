package com.mitaoe.shridhar202401040197.ui.dashboard;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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

import com.mitaoe.shridhar202401040197.data.preference.PreferenceManager;
import com.mitaoe.shridhar202401040197.util.VoiceAssistantHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.mitaoe.shridhar202401040197.util.AppExecutors;

public class DashboardFragment extends Fragment {

    private TextView txtDashboardGreeting;
    private TextView txtBriefingContent;
    private ProgressBar progressBriefing;
    private ImageView btnRefreshBriefing;
    private View btnPlayAudioBriefing;
    private TextView txtMetricCompleted, txtMetricPending, txtMetricNotes, txtMetricModels;
    private TextView txtNoTasksToday;
    private RecyclerView recyclerDashboardTasks;
    private TaskAdapter taskAdapter;

    private AppDatabase db;
    private AiService aiService;
    private PreferenceManager prefManager;
    private VoiceAssistantHelper voiceHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        db = AppDatabase.getInstance(requireContext());
        aiService = new AiService(requireContext());
        prefManager = new PreferenceManager(requireContext());
        voiceHelper = new VoiceAssistantHelper(requireContext());

        txtDashboardGreeting = view.findViewById(R.id.txtDashboardGreeting);
        txtBriefingContent = view.findViewById(R.id.txtBriefingContent);
        progressBriefing = view.findViewById(R.id.progressBriefing);
        btnRefreshBriefing = view.findViewById(R.id.btnRefreshBriefing);
        btnPlayAudioBriefing = view.findViewById(R.id.btnPlayAudioBriefing);
        txtMetricCompleted = view.findViewById(R.id.txtMetricCompleted);
        txtMetricPending = view.findViewById(R.id.txtMetricPending);
        txtMetricNotes = view.findViewById(R.id.txtMetricNotes);
        txtMetricModels = view.findViewById(R.id.txtMetricModels);
        txtNoTasksToday = view.findViewById(R.id.txtNoTasksToday);
        recyclerDashboardTasks = view.findViewById(R.id.recyclerDashboardTasks);

        updateGreeting();
        updateModelsMetric();

        if (btnPlayAudioBriefing != null) {
            btnPlayAudioBriefing.setOnClickListener(v -> {
                String text = txtBriefingContent.getText().toString();
                if (voiceHelper != null && !text.isEmpty()) {
                    voiceHelper.speak(text);
                }
            });
        }

        recyclerDashboardTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        taskAdapter = new TaskAdapter(new TaskAdapter.TaskActionListener() {
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
        recyclerDashboardTasks.setAdapter(taskAdapter);

        if (btnRefreshBriefing != null) {
            btnRefreshBriefing.setOnClickListener(v -> {
                btnRefreshBriefing.animate().rotationBy(360f).setDuration(400).start();
                generateAiBriefing(true);
            });
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Metrics Observation
        db.taskDao().getCompletedTasks().observe(getViewLifecycleOwner(), completed -> {
            if (!isAdded() || getView() == null) return;
            if (txtMetricCompleted != null) {
                txtMetricCompleted.setText(String.valueOf(completed != null ? completed.size() : 0));
            }
        });

        db.taskDao().getPendingTasks().observe(getViewLifecycleOwner(), pending -> {
            if (!isAdded() || getView() == null) return;
            if (txtMetricPending != null) {
                txtMetricPending.setText(String.valueOf(pending != null ? pending.size() : 0));
            }
        });

        db.noteDao().getAllNotes().observe(getViewLifecycleOwner(), notes -> {
            if (!isAdded() || getView() == null) return;
            if (txtMetricNotes != null) {
                txtMetricNotes.setText(String.valueOf(notes != null ? notes.size() : 0));
            }
        });

        // Tasks Due Today Observation
        db.taskDao().getTasksDueToday(DateTimeUtil.getEndOfTodayMillis()).observe(getViewLifecycleOwner(), tasks -> {
            if (!isAdded() || getView() == null) return;
            if (tasks != null) {
                if (taskAdapter != null) taskAdapter.setTasks(tasks);
                if (txtNoTasksToday != null) {
                    txtNoTasksToday.setVisibility(tasks.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }
        });

        generateAiBriefing(false);
    }

    private void generateAiBriefing(boolean forceRefresh) {
        if (!isAdded() || getView() == null) return;
        if (progressBriefing != null) progressBriefing.setVisibility(View.VISIBLE);
        if (txtBriefingContent != null) txtBriefingContent.setText("Preparing your personalized morning briefing...");

        AppExecutors.getInstance().diskIO().execute(() -> {
            List<TaskItem> pending = db.taskDao().getPendingTasksSync();
            List<NoteItem> notes = db.noteDao().getAllNotesSync();

            String todayDate = DateTimeUtil.formatDate(System.currentTimeMillis());
            String activeModel = prefManager.getActiveModelId();
            String signature = todayDate + "_" + pending.size() + "_" + notes.size() + "_" + activeModel;

            if (!forceRefresh) {
                String cachedSig = prefManager.getCachedBriefingSignature();
                String cachedText = prefManager.getCachedBriefingText();
                if (signature.equals(cachedSig) && cachedText != null && !cachedText.isEmpty()) {
                    Activity act = getActivity();
                    if (isAdded() && act != null && !act.isFinishing() && !act.isDestroyed()) {
                        act.runOnUiThread(() -> {
                            if (!isAdded() || getView() == null) return;
                            if (progressBriefing != null) progressBriefing.setVisibility(View.GONE);
                            if (txtBriefingContent != null) txtBriefingContent.setText(cachedText);
                        });
                    }
                    return;
                }
            }

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
                    if (fullResponse != null && !fullResponse.trim().isEmpty()) {
                        prefManager.saveCachedBriefing(signature, fullResponse);
                    }
                    if (isAdded() && getView() != null) {
                        if (progressBriefing != null) progressBriefing.setVisibility(View.GONE);
                        if (txtBriefingContent != null) txtBriefingContent.setText(fullResponse);
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    if (isAdded() && getView() != null) {
                        if (progressBriefing != null) progressBriefing.setVisibility(View.GONE);
                        if (txtBriefingContent != null) {
                            txtBriefingContent.setText("Good day! You have " + pending.size() + " pending tasks. Let's make today productive and check off your priorities.");
                        }
                    }
                }
            });
        });
    }

    private void updateGreeting() {
        if (txtDashboardGreeting == null) return;
        Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        String timeGreeting = "Good day";
        if (hour >= 4 && hour < 12) {
            timeGreeting = "Good morning";
        } else if (hour >= 12 && hour < 17) {
            timeGreeting = "Good afternoon";
        } else if (hour >= 17 && hour < 22) {
            timeGreeting = "Good evening";
        }
        String name = prefManager.getUserName();
        txtDashboardGreeting.setText(timeGreeting + ", " + name);
    }

    private void updateModelsMetric() {
        if (txtMetricModels == null) return;
        int pinnedCount = prefManager.getPinnedModels().size();
        txtMetricModels.setText(String.valueOf(pinnedCount));
    }

    @Override
    public void onResume() {
        super.onResume();
        updateGreeting();
        updateModelsMetric();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (voiceHelper != null) {
            voiceHelper.destroy();
            voiceHelper = null;
        }
    }
}
