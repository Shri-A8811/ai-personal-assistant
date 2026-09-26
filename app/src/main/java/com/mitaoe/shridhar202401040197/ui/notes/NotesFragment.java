package com.mitaoe.shridhar202401040197.ui.notes;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.mitaoe.shridhar202401040197.R;
import com.mitaoe.shridhar202401040197.ai.AiService;
import com.mitaoe.shridhar202401040197.data.database.AppDatabase;
import com.mitaoe.shridhar202401040197.data.model.NoteItem;
import com.mitaoe.shridhar202401040197.data.model.TaskItem;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class NotesFragment extends Fragment {

    private RecyclerView recyclerNotes;
    private NoteAdapter adapter;
    private TextView txtEmptyNotes;
    private EditText editSearchNotes;
    private AppDatabase db;
    private AiService aiService;
    private LiveData<List<NoteItem>> currentLiveData;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notes, container, false);

        db = AppDatabase.getInstance(requireContext());
        aiService = new AiService(requireContext());

        recyclerNotes = view.findViewById(R.id.recyclerNotes);
        txtEmptyNotes = view.findViewById(R.id.txtEmptyNotes);
        editSearchNotes = view.findViewById(R.id.editSearchNotes);
        ExtendedFloatingActionButton fabAddNote = view.findViewById(R.id.fabAddNote);

        recyclerNotes.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new NoteAdapter(new NoteAdapter.NoteActionListener() {
            @Override
            public void onNoteClick(NoteItem note) {
                Intent intent = new Intent(requireContext(), NoteEditActivity.class);
                intent.putExtra(NoteEditActivity.EXTRA_NOTE, note);
                startActivity(intent);
            }

            @Override
            public void onNoteDelete(NoteItem note) {
                Executors.newSingleThreadExecutor().execute(() -> db.noteDao().delete(note));
                Toast.makeText(requireContext(), "Note deleted", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAiSummarizeClick(NoteItem note) {
                String prompt = "Summarize this note in 2-3 bullet points:\n\n" + note.getContent();
                aiService.generateResponse(new ArrayList<>(), prompt, new AiService.StreamCallback() {
                    @Override public void onStart() {}
                    @Override public void onToken(String token, String fullTextSoFar) {}

                    @Override
                    public void onComplete(String fullResponse) {
                        new AlertDialog.Builder(requireContext())
                                .setTitle("✨ Summary: " + note.getTitle())
                                .setMessage(fullResponse)
                                .setPositiveButton("Close", null)
                                .show();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Toast.makeText(requireContext(), "AI Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onAiExtractTasksClick(NoteItem note) {
                String prompt = "Extract actionable tasks from this note (one per line):\n\n" + note.getContent();
                aiService.generateResponse(new ArrayList<>(), prompt, new AiService.StreamCallback() {
                    @Override public void onStart() {}
                    @Override public void onToken(String token, String fullTextSoFar) {}

                    @Override
                    public void onComplete(String fullResponse) {
                        String[] lines = fullResponse.split("\n");
                        int count = 0;
                        for (String line : lines) {
                            String clean = line.replaceAll("^[0-9]+[.\\-\\s]+", "").trim();
                            if (!clean.isEmpty()) {
                                TaskItem task = new TaskItem(clean, "From note: " + note.getTitle(), 0, "MEDIUM", "From Notes", false);
                                Executors.newSingleThreadExecutor().execute(() -> db.taskDao().insert(task));
                                count++;
                            }
                        }
                        Toast.makeText(requireContext(), "Extracted " + count + " tasks to your Tasks tab! ✅", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Toast.makeText(requireContext(), "AI Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        recyclerNotes.setAdapter(adapter);

        fabAddNote.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), NoteEditActivity.class);
            startActivity(intent);
        });

        editSearchNotes.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (query.isEmpty()) {
                    observeNotes(db.noteDao().getAllNotes());
                } else {
                    observeNotes(db.noteDao().searchNotes(query));
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        observeNotes(db.noteDao().getAllNotes());

        return view;
    }

    private void observeNotes(LiveData<List<NoteItem>> liveData) {
        if (currentLiveData != null) {
            currentLiveData.removeObservers(getViewLifecycleOwner());
        }
        currentLiveData = liveData;
        currentLiveData.observe(getViewLifecycleOwner(), notes -> {
            if (notes != null) {
                adapter.setNotes(notes);
                txtEmptyNotes.setVisibility(notes.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });
    }
}
