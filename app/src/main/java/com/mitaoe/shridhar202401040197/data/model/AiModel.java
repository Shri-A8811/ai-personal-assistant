package com.mitaoe.shridhar202401040197.data.model;

import java.io.Serializable;

public class AiModel implements Serializable {
    private String id;
    private String displayName;
    private String provider; // "openrouter", "gemini", "groq", "nim", "openai"
    private boolean isFree;
    private boolean isPinned;
    private String description;

    public AiModel(String id, String displayName, String provider, boolean isFree, boolean isPinned, String description) {
        this.id = id;
        this.displayName = displayName;
        this.provider = provider;
        this.isFree = isFree;
        this.isPinned = isPinned;
        this.description = description;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public boolean isFree() { return isFree; }
    public void setFree(boolean free) { isFree = free; }

    public boolean isPinned() { return isPinned; }
    public void setPinned(boolean pinned) { isPinned = pinned; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
