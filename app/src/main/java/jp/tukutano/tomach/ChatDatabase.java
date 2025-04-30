package jp.tukutano.tomach;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import jp.tukutano.tomach.db.ChatMessage;
import jp.tukutano.tomach.db.ChatMessageDao;

@Database(entities = {ChatMessage.class}, version = 1)
public abstract class ChatDatabase extends RoomDatabase {
    public abstract ChatMessageDao chatMessageDao();

    private static volatile ChatDatabase INSTANCE;

    public static ChatDatabase getInstance(Context ctx) {
        if (INSTANCE == null) {
            synchronized (ChatDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(ctx.getApplicationContext(),
                                    ChatDatabase.class, "chat_db")
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
