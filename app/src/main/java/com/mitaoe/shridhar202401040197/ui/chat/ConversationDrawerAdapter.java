package com.mitaoe.shridhar202401040197.ui.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mitaoe.shridhar202401040197.R;
import com.mitaoe.shridhar202401040197.data.model.Conversation;
import com.mitaoe.shridhar202401040197.util.DateTimeUtil;

import java.util.ArrayList;
import java.util.List;

public class ConversationDrawerAdapter extends RecyclerView.Adapter<ConversationDrawerAdapter.ViewHolder> {

    public interface OnConversationClickListener {
        void onConversationClick(Conversation conversation);
        void onConversationDelete(Conversation conversation);
    }

    private List<Conversation> conversations = new ArrayList<>();
    private long selectedConversationId = -1;
    private final OnConversationClickListener listener;

    public ConversationDrawerAdapter(OnConversationClickListener listener) {
        this.listener = listener;
    }

    public void setConversations(List<Conversation> list, long selectedId) {
        this.conversations = list != null ? list : new ArrayList<>();
        this.selectedConversationId = selectedId;
        notifyDataSetChanged();
    }

    public void setSelectedConversationId(long id) {
        this.selectedConversationId = id;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_drawer_conversation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Conversation conv = conversations.get(position);
        holder.txtTitle.setText(conv.getTitle() != null && !conv.getTitle().isEmpty() ? conv.getTitle() : "Conversation");
        holder.txtTime.setText(DateTimeUtil.formatDate(conv.getUpdatedAt()));

        if (conv.getId() == selectedConversationId) {
            holder.itemView.setBackgroundResource(R.drawable.bg_model_pill);
        } else {
            holder.itemView.setBackgroundResource(R.drawable.bg_chat_user);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onConversationClick(conv);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onConversationDelete(conv);
        });
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtTitle, txtTime;
        ImageView btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTitle = itemView.findViewById(R.id.txtDrawerChatTitle);
            txtTime = itemView.findViewById(R.id.txtDrawerChatTime);
            btnDelete = itemView.findViewById(R.id.btnDeleteConversation);
        }
    }
}
