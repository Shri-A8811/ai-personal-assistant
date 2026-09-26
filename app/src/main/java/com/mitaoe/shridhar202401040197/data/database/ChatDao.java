package com.mitaoe.shridhar202401040197.data.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.mitaoe.shridhar202401040197.data.model.ChatMessage;

import java.util.List;

@Dao
public interface ChatDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(ChatMessage message);

    @androidx.room.Update
    void update(ChatMessage message);

    @Delete
    void delete(ChatMessage message);

    @Query("SELECT * FROM chat_messages WHERE conversationId = :convId ORDER BY timestamp ASC")
    LiveData<List<ChatMessage>> getMessagesForConversation(long convId);

    @Query("SELECT * FROM chat_messages WHERE conversationId = :convId ORDER BY timestamp ASC")
    List<ChatMessage> getMessagesForConversationSync(long convId);

    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    LiveData<List<ChatMessage>> getAllMessages();

    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    List<ChatMessage> getAllMessagesSync();

    @Query("DELETE FROM chat_messages WHERE conversationId = :convId")
    void deleteMessagesForConversation(long convId);

    @Query("DELETE FROM chat_messages")
    void clearAllMessages();
}
