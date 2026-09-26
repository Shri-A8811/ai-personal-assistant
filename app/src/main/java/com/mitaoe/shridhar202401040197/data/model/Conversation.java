package com.mitaoe.shridhar202401040197.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "conversations")
public class Conversation implements Serializable {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String title;
    private long createdAt;
    private long updatedAt;
    private String modelName;

    public Conversation(String title, long createdAt, long updatedAt, String modelName) {
        this.title = title;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.modelName = modelName;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
}
