package com.mitaoe.shridhar202401040197.ui.notes;

import android.os.Bundle;
import android.view.View;
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
import com.mitaoe.shridhar202401040197.util.AppExecutors;

import java.util.ArrayList;
import java.util.List;

public class NoteEditActivity extends AppCompatActivity {

    public static final String EXTRA_NOTE = "extra_note";

    private EditText editNoteTitle, editNoteContent;
    private ImageView btnPinNote;
    private NoteItem currentNote;
    private AppDatabase db;
    private AiService aiService;
    private boolean isPinned = false;
    private String selectedColorHex = "#1E1E1E";
    private AlertDialog activeProgressDialog = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_edit);

        db = AppDatabase.getInstance(this);
        aiService = new AiService(this);

        editNoteTitle = findViewById(R.id.editNoteTitle);
        editNoteContent = findViewById(R.id.editNoteContent);
        btnPinNote = findViewById(R.id.btnPinNote);
        ImageView btnBack = findViewById(R.id.btnBack);
        Button btnSaveNote = findViewById(R.id.btnSaveNote);

        Button btnAiSummarize = findViewById(R.id.btnAiSummarize);
        Button btnAiExtractTasks = findViewById(R.id.btnAiExtractTasks);
        Button btnAiPolish = findViewById(R.id.btnAiPolish);

        currentNote = (NoteItem) getIntent().getSerializableExtra(EXTRA_NOTE);
        if (currentNote != null) {
            editNoteTitle.setText(currentNote.getTitle());
            editNoteContent.setText(currentNote.getContent());
            isPinned = currentNote.isPinned();
            if (currentNote.getColorHex() != null) {
                selectedColorHex = currentNote.getColorHex();
            }
        }
        updatePinIcon();

        if (btnPinNote != null) {
            btnPinNote.setOnClickListener(v -> {
                isPinned = !isPinned;
                updatePinIcon();
                Toast.makeText(this, isPinned ? "Note pinned to top 📌" : "Note unpinned", Toast.LENGTH_SHORT).show();
            });
        }

        setupColorPickers();

        btnBack.setOnClickListener(v -> finish());
        btnSaveNote.setOnClickListener(v -> saveNoteAndExit());

        btnAiSummarize.setOnClickListener(v -> runAiSummarize());
        btnAiExtractTasks.setOnClickListener(v -> runAiExtractTasks());
        btnAiPolish.setOnClickListener(v -> runAiPolish());
    }

    private void updatePinIcon() {
        if (btnPinNote != null) {
            if (isPinned) {
                btnPinNote.setColorFilter(androidx.core.content.ContextCompat.getColor(this, R.color.electric_cyan));
            } else {
                btnPinNote.setColorFilter(androidx.core.content.ContextCompat.getColor(this, R.color.text_muted));
            }
        }
    }

    private void setupColorPickers() {
        int[] ids = {R.id.colorDefault, R.id.colorBlue, R.id.colorGreen, R.id.colorPurple, R.id.colorAmber, R.id.colorCrimson};
        String[] colors = {"#1E1E1E", "#1E3A8A", "#065F46", "#581C87", "#78350F", "#881337"};
        View[] swatches = new View[ids.length];
        for (int i = 0; i < ids.length; i++) {
            swatches[i] = findViewById(ids[i]);
            final String col = colors[i];
            if (swatches[i] != null) {
                swatches[i].setOnClickListener(view -> {
                    selectedColorHex = col;
                    updateSwatchSelection(swatches, colors);
                });
            }
        }
        updateSwatchSelection(swatches, colors);
    }

    private void updateSwatchSelection(View[] swatches, String[] colors) {
        int matchedIdx = 0;
        for (int i = 0; i < colors.length; i++) {
            if (colors[i].equalsIgnoreCase(selectedColorHex)) {
                matchedIdx = i;
                break;
            }
        }
        for (int i = 0; i < swatches.length; i++) {
            View v = swatches[i];
            if (v == null) continue;
            boolean selected = (i == matchedIdx);
            v.setScaleX(selected ? 1.2f : 1.0f);
            v.setScaleY(selected ? 1.2f : 1.0f);
            v.setAlpha(selected ? 1.0f : 0.65f);
            v.setForeground(selected ? androidx.core.content.ContextCompat.getDrawable(this, R.drawable.bg_swatch_selected) : null);
        }
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
        AppExecutors.getInstance().diskIO().execute(() -> {
            if (currentNote == null) {
                NoteItem newNote = new NoteItem(finalTitle, content, System.currentTimeMillis(), "General", selectedColorHex);
                newNote.setPinned(isPinned);
                db.noteDao().insert(newNote);
            } else {
                currentNote.setTitle(finalTitle);
                currentNote.setContent(content);
                currentNote.setColorHex(selectedColorHex);
                currentNote.setPinned(isPinned);
                currentNote.setUpdatedAtMillis(System.currentTimeMillis());
                db.noteDao().update(currentNote);
            }
            runOnUiThread(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    Toast.makeText(this, "Note saved!", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        });
    }

    private void dismissProgressDialogSafely() {
        if (activeProgressDialog != null) {
            try {
                if (activeProgressDialog.isShowing()) {
                    activeProgressDialog.dismiss();
                }
            } catch (Exception ignored) {}
            activeProgressDialog = null;
        }
    }

    @Override
    protected void onDestroy() {
        dismissProgressDialogSafely();
        super.onDestroy();
    }

    private AlertDialog createLoadingDialog(String message) {
        dismissProgressDialogSafely();
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
        activeProgressDialog = dialog;
        return dialog;
    }

    private void runAiSummarize() {
        String text = editNoteContent.getText().toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(this, "Write some content in the note first.", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog progress = createLoadingDialog("Summarizing note...");
        if (!isFinishing() && !isDestroyed()) {
            progress.show();
        }

        String prompt = "Summarize this note in 2-3 concise bullet points:\n\n" + text;
        aiService.generateResponse(new ArrayList<>(), prompt, new AiService.StreamCallback() {
            @Override public void onStart() {}
            @Override public void onToken(String token, String fullTextSoFar) {}

            @Override
            public void onComplete(String fullResponse) {
                dismissProgressDialogSafely();
                if (!isFinishing() && !isDestroyed()) {
                    new AlertDialog.Builder(NoteEditActivity.this)
                            .setTitle("Note Summary")
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
                dismissProgressDialogSafely();
                if (!isFinishing() && !isDestroyed()) {
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

        AlertDialog progress = createLoadingDialog("Extracting action items...");
        if (!isFinishing() && !isDestroyed()) {
            progress.show();
        }

        String prompt = "Extract all actionable tasks from this note as a numbered list (one task per line):\n\n" + text;
        aiService.generateResponse(new ArrayList<>(), prompt, new AiService.StreamCallback() {
            @Override public void onStart() {}
            @Override public void onToken(String token, String fullTextSoFar) {}

            @Override
            public void onComplete(String fullResponse) {
                dismissProgressDialogSafely();
                if (!isFinishing() && !isDestroyed()) {
                    String[] lines = fullResponse.split("\n");
                    int added = 0;
                    for (String line : lines) {
                        String clean = line.replaceAll("^[0-9]+[.\\-\\s]+", "").trim();
                        if (!clean.isEmpty()) {
                            TaskItem task = new TaskItem(clean, "Extracted from note: " + editNoteTitle.getText().toString(), 0, "MEDIUM", "From Notes", false);
                            AppExecutors.getInstance().diskIO().execute(() -> db.taskDao().insert(task));
                            added++;
                        }
                    }
                    Toast.makeText(NoteEditActivity.this, "Created " + added + " tasks from this note", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                dismissProgressDialogSafely();
                if (!isFinishing() && !isDestroyed()) {
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

        AlertDialog progress = createLoadingDialog("Polishing your note...");
        if (!isFinishing() && !isDestroyed()) {
            progress.show();
        }

        String prompt = "Rewrite and polish this note text to improve grammar, clarity, and formatting without changing its meaning:\n\n" + text;
        aiService.generateResponse(new ArrayList<>(), prompt, new AiService.StreamCallback() {
            @Override public void onStart() {}
            @Override public void onToken(String token, String fullTextSoFar) {}

            @Override
            public void onComplete(String fullResponse) {
                dismissProgressDialogSafely();
                if (!isFinishing() && !isDestroyed()) {
                    editNoteContent.setText(fullResponse);
                    Toast.makeText(NoteEditActivity.this, "Note polished", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                dismissProgressDialogSafely();
                if (!isFinishing() && !isDestroyed()) {
                    Toast.makeText(NoteEditActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
