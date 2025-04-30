package jp.tukutano.tomach.ui.home;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import jp.tukutano.tomach.db.ChatMessage;
import jp.tukutano.tomach.db.ChatRepository;

public class HomeViewModel extends AndroidViewModel {

    private ChatRepository repo;
    private LiveData<List<ChatMessage>> messages;

    public HomeViewModel(@NonNull Application app) {
        super(app);
        repo = new ChatRepository(app);
        messages = repo.getAllMessages();
    }

    public LiveData<List<ChatMessage>> getMessages() {
        return messages;
    }

    public void insert(ChatMessage msg) {
        repo.insert(msg);
    }

    public void delete(ChatMessage msg) {
        repo.delete(msg);
    }
}