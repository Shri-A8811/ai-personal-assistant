package com.mitaoe.shridhar202401040197.util;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mitaoe.shridhar202401040197.data.database.AppDatabase;
import com.mitaoe.shridhar202401040197.data.model.ChatMessage;
import com.mitaoe.shridhar202401040197.data.model.Conversation;
import com.mitaoe.shridhar202401040197.data.model.NoteItem;
import com.mitaoe.shridhar202401040197.data.model.TaskItem;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BackupManager {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public interface ImportCallback {
        void onSuccess(int tasksCount, int notesCount);
        void onError(String message);
    }

    public static void exportBackupToShare(Context context) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(context);
                List<TaskItem> tasks = db.taskDao().getAllTasksSync();
                List<NoteItem> notes = db.noteDao().getAllNotesSync();
                List<Conversation> convs = db.conversationDao().getAllConversationsSync();

                JSONObject root = new JSONObject();
                root.put("version", 1);
                root.put("exportedAt", System.currentTimeMillis());
                root.put("tasks", new JSONArray(gson.toJson(tasks)));
                root.put("notes", new JSONArray(gson.toJson(notes)));
                root.put("conversations", new JSONArray(gson.toJson(convs)));

                String jsonOutput = root.toString(2);

                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                String filename = "AIAssistant_Backup_" + timeStamp + ".json";

                File cacheFile = new File(context.getCacheDir(), filename);
                try (FileOutputStream fos = new FileOutputStream(cacheFile)) {
                    fos.write(jsonOutput.getBytes(StandardCharsets.UTF_8));
                }

                final Context appContext = context.getApplicationContext();
                AppExecutors.getInstance().mainThread().execute(() -> {
                    try {
                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.setType("application/json");
                        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "AI Assistant Data Backup");
                        shareIntent.putExtra(Intent.EXTRA_TEXT, jsonOutput);
                        Intent chooser = Intent.createChooser(shareIntent, "Save or Export Backup JSON");
                        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        appContext.startActivity(chooser);
                    } catch (Exception e) {
                        Toast.makeText(appContext, "Export error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                AppExecutors.getInstance().mainThread().execute(() -> {
                    Toast.makeText(context, "Backup failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    public static void importBackupFromJson(Context context, String jsonContent, ImportCallback callback) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                JSONObject root = new JSONObject(jsonContent);
                AppDatabase db = AppDatabase.getInstance(context);

                int importedTasks = 0;
                int importedNotes = 0;

                if (root.has("tasks")) {
                    Type taskListType = new TypeToken<List<TaskItem>>(){}.getType();
                    List<TaskItem> tasks = gson.fromJson(root.getString("tasks"), taskListType);
                    if (tasks != null) {
                        for (TaskItem t : tasks) {
                            t.setId(0); // auto-generate new id
                            if (t.getTitle() == null || t.getTitle().trim().isEmpty()) {
                                t.setTitle("Imported Task");
                            }
                            if (t.getDescription() == null) {
                                t.setDescription("");
                            }
                            if (t.getPriority() == null || t.getPriority().trim().isEmpty()) {
                                t.setPriority("MEDIUM");
                            }
                            if (t.getCategory() == null || t.getCategory().trim().isEmpty()) {
                                t.setCategory("General");
                            }
                            if (t.getRecurrence() == null || t.getRecurrence().trim().isEmpty()) {
                                t.setRecurrence("NONE");
                            }
                            db.taskDao().insert(t);
                            importedTasks++;
                        }
                    }
                }

                if (root.has("notes")) {
                    Type noteListType = new TypeToken<List<NoteItem>>(){}.getType();
                    List<NoteItem> notes = gson.fromJson(root.getString("notes"), noteListType);
                    if (notes != null) {
                        for (NoteItem n : notes) {
                            n.setId(0); // auto-generate new id
                            if (n.getTitle() == null || n.getTitle().trim().isEmpty()) {
                                n.setTitle("Imported Note");
                            }
                            if (n.getContent() == null) {
                                n.setContent("");
                            }
                            if (n.getColorHex() == null || n.getColorHex().trim().isEmpty()) {
                                n.setColorHex("#1E1E1E");
                            }
                            if (n.getCategory() == null || n.getCategory().trim().isEmpty()) {
                                n.setCategory("General");
                            }
                            db.noteDao().insert(n);
                            importedNotes++;
                        }
                    }
                }

                final int finalTasks = importedTasks;
                final int finalNotes = importedNotes;
                AppExecutors.getInstance().mainThread().execute(() -> {
                    callback.onSuccess(finalTasks, finalNotes);
                });

            } catch (Exception e) {
                AppExecutors.getInstance().mainThread().execute(() -> {
                    callback.onError("Invalid backup file: " + e.getMessage());
                });
            }
        });
    }
}
