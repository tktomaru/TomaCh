package jp.tukutano.tomach.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
@Entity(tableName = "chat_messages")
public class ChatMessage {
    @PrimaryKey(autoGenerate = true)
    @NonNull
    public long id;

    public String role;        // "user" または "assistant"
    public String contentJa;   // 日本語
    public String contentEn;   // 英語訳
    public long timestamp;     // タイムスタンプ（ソート用）
}