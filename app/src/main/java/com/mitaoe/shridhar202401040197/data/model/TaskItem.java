package com.mitaoe.shridhar202401040197.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "tasks")
public class TaskItem implements Serializable {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String title;
    private String description;
    private long dueDateMillis;
    private String priority; // HIGH, MEDIUM, LOW
    private boolean isCompleted;
    private String category;
    private boolean hasReminder;

    public TaskItem(String title, String description, long dueDateMillis, String priority, String category, boolean hasReminder) {
        this.title = title;
        this.description = description;
        this.dueDateMillis = dueDateMillis;
        this.priority = priority != null ? priority : "MEDIUM";
        this.isCompleted = false;
        this.category = category != null ? category : "General";
        this.hasReminder = hasReminder;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public long getDueDateMillis() { return dueDateMillis; }
    public void setDueDateMillis(long dueDateMillis) { this.dueDateMillis = dueDateMillis; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public boolean isHasReminder() { return hasReminder; }
    public void setHasReminder(boolean hasReminder) { this.hasReminder = hasReminder; }
}
