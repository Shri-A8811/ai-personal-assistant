package com.mitaoe.shridhar202401040197.data.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.mitaoe.shridhar202401040197.data.model.NoteItem;

import java.util.List;

@Dao
public interface NoteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(NoteItem note);

    @Update
    void update(NoteItem note);

    @Delete
    void delete(NoteItem note);

    @Query("SELECT * FROM notes ORDER BY updatedAtMillis DESC")
    LiveData<List<NoteItem>> getAllNotes();

    @Query("SELECT * FROM notes ORDER BY updatedAtMillis DESC")
    List<NoteItem> getAllNotesSync();

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    NoteItem getNoteById(long id);

    @Query("SELECT * FROM notes WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' ORDER BY updatedAtMillis DESC")
    LiveData<List<NoteItem>> searchNotes(String query);
}
