package jp.tukutano.tomach.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    LiveData<List<ChatMessage>> getAll();

    @Insert
    void insert(ChatMessage msg);

    @Delete
    void delete(ChatMessage msg);
}
