package com.mitaoe.shridhar202401040197.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "chat_messages")
public class ChatMessage implements Serializable {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private long conversationId;
    private String text;
    private boolean isUser;
    private long timestamp;
    private String modelName;
    private boolean hasAction;
    private String actionType; // "TASK_CREATED", "REMINDER_SET", "NOTE_CREATED"
    private String actionTitle;
    private String imageUri; // Optional image attachment

    public ChatMessage(String text, boolean isUser, long timestamp, String modelName) {
        this.conversationId = 0;
        this.text = text;
        this.isUser = isUser;
        this.timestamp = timestamp;
        this.modelName = modelName;
        this.hasAction = false;
    }

    public long getConversationId() { return conversationId; }
    public void setConversationId(long conversationId) { this.conversationId = conversationId; }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public boolean isUser() { return isUser; }
    public void setUser(boolean user) { isUser = user; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public boolean isHasAction() { return hasAction; }
    public void setHasAction(boolean hasAction) { this.hasAction = hasAction; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public String getActionTitle() { return actionTitle; }
    public void setActionTitle(String actionTitle) { this.actionTitle = actionTitle; }

    public String getImageUri() { return imageUri; }
    public void setImageUri(String imageUri) { this.imageUri = imageUri; }
}
