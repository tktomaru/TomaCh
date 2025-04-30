package jp.tukutano.tomach.db;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.Executors;

import jp.tukutano.tomach.ChatDatabase;

public class ChatRepository {
    private ChatMessageDao dao;
    private LiveData<List<ChatMessage>> allMessages;

    public ChatRepository(Application app) {
        ChatDatabase db = ChatDatabase.getInstance(app);
        dao = db.chatMessageDao();
        allMessages = dao.getAll();
    }

    public LiveData<List<ChatMessage>> getAllMessages() {
        return allMessages;
    }

    public void insert(ChatMessage msg) {
        Executors.newSingleThreadExecutor()
                .execute(() -> dao.insert(msg));
    }

    public void delete(ChatMessage msg) {
        Executors.newSingleThreadExecutor()
                .execute(() -> dao.delete(msg));
    }
}

