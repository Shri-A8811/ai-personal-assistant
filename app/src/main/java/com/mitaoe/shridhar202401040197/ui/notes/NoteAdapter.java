package com.mitaoe.shridhar202401040197.ui.notes;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mitaoe.shridhar202401040197.R;
import com.mitaoe.shridhar202401040197.data.model.NoteItem;
import com.mitaoe.shridhar202401040197.util.DateTimeUtil;

import java.util.ArrayList;
import java.util.List;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

    public interface NoteActionListener {
        void onNoteClick(NoteItem note);
        void onNoteDelete(NoteItem note);
        void onAiSummarizeClick(NoteItem note);
        void onAiExtractTasksClick(NoteItem note);
    }

    private List<NoteItem> notes = new ArrayList<>();
    private final NoteActionListener listener;

    public NoteAdapter(NoteActionListener listener) {
        this.listener = listener;
    }

    public void setNotes(List<NoteItem> notes) {
        this.notes = notes != null ? notes : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        NoteItem note = notes.get(position);

        holder.txtNoteTitle.setText(note.getTitle() != null && !note.getTitle().isEmpty() ? note.getTitle() : "Untitled Note");
        holder.txtNoteContent.setText(note.getContent() != null ? note.getContent() : "");
        holder.txtNoteDate.setText(DateTimeUtil.formatDate(note.getUpdatedAtMillis()));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onNoteClick(note);
        });

        holder.btnDeleteNote.setOnClickListener(v -> {
            if (listener != null) listener.onNoteDelete(note);
        });

        holder.chipAiSummarize.setOnClickListener(v -> {
            if (listener != null) listener.onAiSummarizeClick(note);
        });

        holder.chipAiTasks.setOnClickListener(v -> {
            if (listener != null) listener.onAiExtractTasksClick(note);
        });
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView txtNoteTitle, txtNoteContent, txtNoteDate;
        TextView chipAiSummarize, chipAiTasks;
        ImageView btnDeleteNote;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            txtNoteTitle = itemView.findViewById(R.id.txtNoteTitle);
            txtNoteContent = itemView.findViewById(R.id.txtNoteContent);
            txtNoteDate = itemView.findViewById(R.id.txtNoteDate);
            chipAiSummarize = itemView.findViewById(R.id.chipAiSummarize);
            chipAiTasks = itemView.findViewById(R.id.chipAiTasks);
            btnDeleteNote = itemView.findViewById(R.id.btnDeleteNote);
        }
    }
}
