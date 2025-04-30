package jp.tukutano.tomach.ui.dashboard;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import jp.tukutano.tomach.db.ChatMessage;
import jp.tukutano.tomach.db.ChatRepository;

public class DashboardViewModel extends AndroidViewModel {
    public DashboardViewModel(@NonNull Application app) {
        super(app);
    }
}