package com.mitaoe.shridhar202401040197.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "notes")
public class NoteItem implements Serializable {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String title;
    private String content;
    private long updatedAtMillis;
    private String category;
    private String colorHex;

    public NoteItem(String title, String content, long updatedAtMillis, String category, String colorHex) {
        this.title = title;
        this.content = content;
        this.updatedAtMillis = updatedAtMillis;
        this.category = category != null ? category : "General";
        this.colorHex = colorHex != null ? colorHex : "#1E1E1E";
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public long getUpdatedAtMillis() { return updatedAtMillis; }
    public void setUpdatedAtMillis(long updatedAtMillis) { this.updatedAtMillis = updatedAtMillis; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getColorHex() { return colorHex; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }
}
