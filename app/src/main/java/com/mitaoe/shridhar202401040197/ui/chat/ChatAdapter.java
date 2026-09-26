package com.mitaoe.shridhar202401040197.ui.chat;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mitaoe.shridhar202401040197.R;
import com.mitaoe.shridhar202401040197.data.model.ChatMessage;
import com.mitaoe.shridhar202401040197.util.DateTimeUtil;

import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_ASSISTANT = 2;

    public interface OnSpeakListener {
        void onSpeak(String text);
    }

    private List<ChatMessage> messages = new ArrayList<>();
    private final OnSpeakListener speakListener;

    public ChatAdapter(OnSpeakListener speakListener) {
        this.speakListener = speakListener;
    }

    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages != null ? messages : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void appendMessage(ChatMessage message) {
        this.messages.add(message);
        notifyItemInserted(this.messages.size() - 1);
    }

    public void updateLastMessage(String updatedText) {
        if (!messages.isEmpty()) {
            int lastIndex = messages.size() - 1;
            messages.get(lastIndex).setText(updatedText);
            notifyItemChanged(lastIndex);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).isUser() ? VIEW_TYPE_USER : VIEW_TYPE_ASSISTANT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_USER) {
            View view = inflater.inflate(R.layout.item_chat_user, parent, false);
            return new UserViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_chat_assistant, parent, false);
            return new AssistantViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage msg = messages.get(position);
        if (holder instanceof UserViewHolder) {
            ((UserViewHolder) holder).bind(msg);
        } else if (holder instanceof AssistantViewHolder) {
            ((AssistantViewHolder) holder).bind(msg, speakListener);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        private final TextView txtUserMessage;
        private final TextView txtUserTime;
        private final ImageView imgUserAttachment;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            txtUserMessage = itemView.findViewById(R.id.txtUserMessage);
            txtUserTime = itemView.findViewById(R.id.txtUserTime);
            imgUserAttachment = itemView.findViewById(R.id.imgUserAttachment);
        }

        public void bind(ChatMessage msg) {
            txtUserMessage.setText(msg.getText());
            txtUserTime.setText(DateTimeUtil.formatTime(msg.getTimestamp()));
            if (msg.getImageUri() != null && !msg.getImageUri().isEmpty()) {
                imgUserAttachment.setVisibility(View.VISIBLE);
                try {
                    imgUserAttachment.setImageURI(Uri.parse(msg.getImageUri()));
                } catch (Exception ignored) {
                    imgUserAttachment.setVisibility(View.GONE);
                }
            } else {
                imgUserAttachment.setVisibility(View.GONE);
            }
        }
    }

    static class AssistantViewHolder extends RecyclerView.ViewHolder {
        private final TextView txtAssistantModel;
        private final TextView txtAssistantTime;
        private final TextView txtAssistantMessage;
        private final View cardActionExecuted;
        private final TextView txtActionDetails;
        private final ImageView btnCopyText;
        private final ImageView btnSpeakText;

        public AssistantViewHolder(@NonNull View itemView) {
            super(itemView);
            txtAssistantModel = itemView.findViewById(R.id.txtAssistantModel);
            txtAssistantTime = itemView.findViewById(R.id.txtAssistantTime);
            txtAssistantMessage = itemView.findViewById(R.id.txtAssistantMessage);
            cardActionExecuted = itemView.findViewById(R.id.cardActionExecuted);
            txtActionDetails = itemView.findViewById(R.id.txtActionDetails);
            btnCopyText = itemView.findViewById(R.id.btnCopyText);
            btnSpeakText = itemView.findViewById(R.id.btnSpeakText);
        }

        public void bind(ChatMessage msg, OnSpeakListener listener) {
            txtAssistantModel.setText(msg.getModelName() != null ? msg.getModelName() : "Assistant");
            txtAssistantTime.setText(DateTimeUtil.formatTime(msg.getTimestamp()));
            int codeColor = androidx.core.content.ContextCompat.getColor(itemView.getContext(), R.color.accent_sparkle);
            txtAssistantMessage.setText(com.mitaoe.shridhar202401040197.util.MarkdownUtil.renderMarkdown(msg.getText(), codeColor));

            if (msg.isHasAction()) {
                cardActionExecuted.setVisibility(View.VISIBLE);
                txtActionDetails.setText(msg.getActionTitle() != null ? msg.getActionTitle() : "Action Executed");
            } else {
                cardActionExecuted.setVisibility(View.GONE);
            }

            btnCopyText.setOnClickListener(v -> {
                Context context = v.getContext();
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Assistant Response", msg.getText());
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show();
                }
            });

            btnSpeakText.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSpeak(msg.getText());
                }
            });
        }
    }
}
