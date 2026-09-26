package com.mitaoe.shridhar202401040197.ui.notes;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.mitaoe.shridhar202401040197.R;
import com.mitaoe.shridhar202401040197.ai.AiService;
import com.mitaoe.shridhar202401040197.data.database.AppDatabase;
import com.mitaoe.shridhar202401040197.data.model.ChatMessage;
import com.mitaoe.shridhar202401040197.data.model.NoteItem;
import com.mitaoe.shridhar202401040197.data.model.TaskItem;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class NoteEditActivity extends AppCompatActivity {

    public static final String EXTRA_NOTE = "extra_note";

    private EditText editNoteTitle, editNoteContent;
    private NoteItem currentNote;
    private AppDatabase db;
    private AiService aiService;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_edit);

        db = AppDatabase.getInstance(this);
        aiService = new AiService(this);

        editNoteTitle = findViewById(R.id.editNoteTitle);
        editNoteContent = findViewById(R.id.editNoteContent);
        ImageView btnBack = findViewById(R.id.btnBack);
        Button btnSaveNote = findViewById(R.id.btnSaveNote);

        Button btnAiSummarize = findViewById(R.id.btnAiSummarize);
        Button btnAiExtractTasks = findViewById(R.id.btnAiExtractTasks);
        Button btnAiPolish = findViewById(R.id.btnAiPolish);

        currentNote = (NoteItem) getIntent().getSerializableExtra(EXTRA_NOTE);
        if (currentNote != null) {
            editNoteTitle.setText(currentNote.getTitle());
            editNoteContent.setText(currentNote.getContent());
        }

        btnBack.setOnClickListener(v -> finish());
        btnSaveNote.setOnClickListener(v -> saveNoteAndExit());

        btnAiSummarize.setOnClickListener(v -> runAiSummarize());
        btnAiExtractTasks.setOnClickListener(v -> runAiExtractTasks());
        btnAiPolish.setOnClickListener(v -> runAiPolish());
    }

    private void saveNoteAndExit() {
        String title = editNoteTitle.getText().toString().trim();
        String content = editNoteContent.getText().toString().trim();

        if (title.isEmpty() && content.isEmpty()) {
            finish();
            return;
        }

        if (title.isEmpty()) {
            title = content.length() > 25 ? content.substring(0, 25) + "..." : content;
        }

        final String finalTitle = title;
        Executors.newSingleThreadExecutor().execute(() -> {
            if (currentNote == null) {
                NoteItem newNote = new NoteItem(finalTitle, content, System.currentTimeMillis(), "General", "#1E1E1E");
                db.noteDao().insert(newNote);
            } else {
                currentNote.setTitle(finalTitle);
                currentNote.setContent(content);
                currentNote.setUpdatedAtMillis(System.currentTimeMillis());
                db.noteDao().update(currentNote);
            }
            runOnUiThread(() -> {
                Toast.makeText(this, "Note saved!", Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }

    private AlertDialog createLoadingDialog(String message) {
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        layout.setPadding(48, 36, 48, 36);
        layout.setGravity(android.view.Gravity.CENTER_VERTICAL);

        android.widget.ProgressBar bar = new android.widget.ProgressBar(this);
        bar.setIndeterminate(true);
        layout.addView(bar);

        android.widget.TextView tv = new android.widget.TextView(this);
        tv.setText("   " + message);
        tv.setTextSize(14);
        tv.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.text_primary));
        layout.addView(tv);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(layout)
                .setCancelable(false)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_chat_assistant);
        }
        return dialog;
    }

    private void runAiSummarize() {
        String text = editNoteContent.getText().toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(this, "Write some content in the note first.", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog progress = createLoadingDialog("✨ AI is summarizing note...");
        progress.show();

        String prompt = "Summarize this note in 2-3 concise bullet points:\n\n" + text;
        aiService.generateResponse(new ArrayList<>(), prompt, new AiService.StreamCallback() {
            @Override public void onStart() {}
            @Override public void onToken(String token, String fullTextSoFar) {}

            @Override
            public void onComplete(String fullResponse) {
                if (!isFinishing() && !isDestroyed()) {
                    progress.dismiss();
                    new AlertDialog.Builder(NoteEditActivity.this)
                            .setTitle("✨ Note Summary")
                            .setMessage(fullResponse)
                            .setPositiveButton("Append to Note", (d, w) -> {
                                editNoteContent.append("\n\n---\n**Summary:**\n" + fullResponse);
                            })
                            .setNegativeButton("Close", null)
                            .show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (!isFinishing() && !isDestroyed()) {
                    progress.dismiss();
                    Toast.makeText(NoteEditActivity.this, "AI error: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void runAiExtractTasks() {
        String text = editNoteContent.getText().toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(this, "Write some content in the note first.", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog progress = createLoadingDialog("📋 AI is extracting action items...");
        progress.show();

        String prompt = "Extract all actionable tasks from this note as a numbered list (one task per line):\n\n" + text;
        aiService.generateResponse(new ArrayList<>(), prompt, new AiService.StreamCallback() {
            @Override public void onStart() {}
            @Override public void onToken(String token, String fullTextSoFar) {}

            @Override
            public void onComplete(String fullResponse) {
                if (!isFinishing() && !isDestroyed()) {
                    progress.dismiss();
                    String[] lines = fullResponse.split("\n");
                    int added = 0;
                    for (String line : lines) {
                        String clean = line.replaceAll("^[0-9]+[.\\-\\s]+", "").trim();
                        if (!clean.isEmpty()) {
                            TaskItem task = new TaskItem(clean, "Extracted from note: " + editNoteTitle.getText().toString(), 0, "MEDIUM", "From Notes", false);
                            Executors.newSingleThreadExecutor().execute(() -> db.taskDao().insert(task));
                            added++;
                        }
                    }
                    Toast.makeText(NoteEditActivity.this, "Created " + added + " tasks from this note! ✅", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (!isFinishing() && !isDestroyed()) {
                    progress.dismiss();
                    Toast.makeText(NoteEditActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void runAiPolish() {
        String text = editNoteContent.getText().toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(this, "Write some content in the note first.", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog progress = createLoadingDialog("✍️ AI is polishing your note...");
        progress.show();

        String prompt = "Rewrite and polish this note text to improve grammar, clarity, and formatting without changing its meaning:\n\n" + text;
        aiService.generateResponse(new ArrayList<>(), prompt, new AiService.StreamCallback() {
            @Override public void onStart() {}
            @Override public void onToken(String token, String fullTextSoFar) {}

            @Override
            public void onComplete(String fullResponse) {
                if (!isFinishing() && !isDestroyed()) {
                    progress.dismiss();
                    editNoteContent.setText(fullResponse);
                    Toast.makeText(NoteEditActivity.this, "Note polished! ✨", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (!isFinishing() && !isDestroyed()) {
                    progress.dismiss();
                    Toast.makeText(NoteEditActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
