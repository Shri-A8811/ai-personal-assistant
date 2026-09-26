package com.mitaoe.shridhar202401040197.data.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.mitaoe.shridhar202401040197.data.model.Conversation;

import java.util.List;

@Dao
public interface ConversationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Conversation conversation);

    @Update
    void update(Conversation conversation);

    @Delete
    void delete(Conversation conversation);

    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    LiveData<List<Conversation>> getAllConversations();

    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    List<Conversation> getAllConversationsSync();

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    Conversation getConversationById(long id);

    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC LIMIT 1")
    Conversation getLatestConversationSync();

    @Query("DELETE FROM chat_messages WHERE conversationId = :convId")
    void deleteMessagesForConversation(long convId);
}
