package jp.tukutano.tomach.ui.home;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import jp.tukutano.tomach.R;
import jp.tukutano.tomach.databinding.FragmentHomeBinding;
import jp.tukutano.tomach.db.ChatMessage;
import jp.tukutano.tomach.ui.ChatAdapter;
import jp.tukutano.tomach.util.LogUtils;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    private static final int PERMISSION_REQUEST_CODE = 100;
    private SpeechRecognizer recognizer;
    private Button btnVoiceStart, btnStopSpeaking;   // 追加
    private Intent recogIntent;
    // 追加：TTS オブジェクト
    private TextToSpeech tts;
    // 追加：手動入力モード切り替え用フラグ
    private boolean isManual = false;

    // 追加：会話履歴を保持する JsonArray
    private JsonArray chatHistory = new JsonArray();
    private static final String PREFS_NAME = "openai_prefs";
    private static final String KEY_SHOW_ENGLISH = "show_english";
    private static final String KEY_API = "api_key";
    private static final String KEY_TEMPERATURE = "temperature";
    private String savedApiKey = "";

    private HomeViewModel viewModel;
    private RecyclerView rvChat;
    private ChatAdapter adapter;
    private double temperature;  // 0.0～1.0

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        LogUtils.logWithCaller(Thread.currentThread().getStackTrace(), "start");
        viewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // RecyclerView のセットアップ
        rvChat = root.findViewById(R.id.rvChat);
        adapter = new ChatAdapter(msg -> viewModel.delete(msg));
        rvChat.setLayoutManager(new LinearLayoutManager(getContext()));
        rvChat.setAdapter(adapter);
        // DB から履歴を監視 → 更新が入るたび RecyclerView に反映
        viewModel.getMessages().observe(getViewLifecycleOwner(), list -> {
            adapter.setItems(list);
            rvChat.scrollToPosition(list.size() - 1);
        });

        btnVoiceStart = root.findViewById(R.id.btnVoiceStart);  // 追加
        btnStopSpeaking = root.findViewById(R.id.btnStopSpeaking); // 追加

        // ■■ システム指示メッセージを最初に追加 ■■
        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", "あなたは日本語で会話するアシスタントです。過去の会話の流れを踏まえて回答してください。");
        chatHistory.add(systemMsg);

        // パーミッション要求
        if (!checkPermission()) requestPermission();

        // SpeechRecognizer 初期化
        recognizer = SpeechRecognizer.createSpeechRecognizer(getContext());
        recognizer.setRecognitionListener(new MyRecognitionListener());

        recogIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recogIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ja-JP");
        recogIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);

        // 日本語の発声
        tts = new TextToSpeech(getContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                // 日本語ロケールを設定
                Locale localeJa = Locale.JAPAN;
                int res = tts.setLanguage(localeJa);
                if (res == TextToSpeech.LANG_MISSING_DATA
                        || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "日本語TTSがサポートされていません");
                } else {
                }
                Log.e("TTS", "TTS 初期化失敗");
            }
        });

        // DB から過去のメッセージを取り出し、chatHistory にセット
        viewModel.getMessages().observe(getViewLifecycleOwner(), list -> {
            // 1) まず chatHistory をクリアしてシステムメッセージだけセット
            chatHistory = new JsonArray();
            chatHistory.add(systemMsg);

            // 2) UI に既存メッセージを表示（起動時の再表示用）
            for (ChatMessage msg : list) {
                // JSON 履歴にも追加
                JsonObject jo = new JsonObject();
                jo.addProperty("role", msg.role);
                jo.addProperty("content", msg.contentJa);
                chatHistory.add(jo);
            }
        });

        // ViewModel の LiveData 監視など…
        viewModel.getMessages().observe(getViewLifecycleOwner(), list -> {
            adapter.setItems(list);
            rvChat.scrollToPosition(list.size() - 1);
        });

        // ボタン押下で音声認識開始
        btnVoiceStart.setOnClickListener(v -> {
            isManual = true;
            startListening();
        });

        // 追加：音声停止ボタン押下でTTSを中断
        btnStopSpeaking.setOnClickListener(v -> {
            if (tts != null) {
                tts.stop();      // 発話を即時キャンセル
            }
        });

        // 既存のキーがあれば表示
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        savedApiKey = prefs.getString(KEY_API, "");


        // SharedPreferences 取得
        SharedPreferences settings = requireContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // 1) 保存済み設定を読み込んで初期状態をセット
        boolean showEnglish = settings.getBoolean(KEY_SHOW_ENGLISH, true);
        adapter.setShowEnglish(showEnglish);

        // 追加：temperature 初期値読み込み（0.8 がデフォルト）
        temperature = Double.longBitsToDouble(
                settings.getLong(KEY_TEMPERATURE, Double.doubleToLongBits(0.8))
        );

        // 最初のリスニング開始
        startListening();
        return root;
    }

    /**
     * 音声認識を開始するメソッド。
     * マイク権限がない場合はリクエストし、権限がある場合のみ startListening を呼び出します。
     */
    private void startListening() {
//        LogUtils.logWithCaller(Thread.currentThread().getStackTrace(), "start");
        // マイク権限をチェック
        if (!checkPermission()) {
            // 権限がなければリクエスト
            requestPermission();
            return;
        }
        // 認識リクエストを開始
        recognizer.startListening(recogIntent);
    }

    private boolean checkPermission() {
//        LogUtils.logWithCaller(Thread.currentThread().getStackTrace(), "start");
        return ContextCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        LogUtils.logWithCaller(Thread.currentThread().getStackTrace(), "start");
        ActivityCompat.requestPermissions(
                getActivity(),
                new String[]{ Manifest.permission.RECORD_AUDIO },
                PERMISSION_REQUEST_CODE
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startListening();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        recognizer.destroy();
        binding = null;
        // --- ここから追加 ---
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        // --- ここまで追加 ---
    }

    private class MyRecognitionListener implements RecognitionListener {
        @Override public void onReadyForSpeech(Bundle params) { }
        @Override public void onBeginningOfSpeech() { }
        @Override public void onRmsChanged(float rmsdB) { }
        @Override public void onBufferReceived(byte[] buffer) { }
        @Override public void onEndOfSpeech() { }
        @Override public void onError(int error) {
            startListening();
        }

        @Override
        public void onResults(Bundle results) {
            LogUtils.logWithCaller(Thread.currentThread().getStackTrace(), "start");
            ArrayList<String> list = results
                    .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (list == null || list.isEmpty()) {
                startListening();
                return;
            }

            String text = list.get(0);
            LogUtils.logWithCaller(Thread.currentThread().getStackTrace(), text);


            if (isManual) {
                // ■■■ 手動モード：そのままクエリとして処理 ■■■
                handleUserQuery(text);
                isManual = false;  // フラグクリア
            } else {
                // ウェイクワード検出（前方一致）
                if (text.startsWith("ジクサー") || text.startsWith("じくさー") || text.startsWith("gixxer")) {
                    // 先頭に “ジクサー” があれば除去
                    text = text.replaceFirst("^ジクサー[ 、]*", "");
                    // 「ヘイ トマ」を除去して実際のクエリを取得
                    String query = text.replaceFirst("ヘイ[ 、]*トマ[ 、]*", "");
                    if (!query.isEmpty()) {
                        handleUserQuery(query);
                    }
                }
            }
            // 常に再リスン
            startListening();
        }

        @Override public void onPartialResults(Bundle partialResults) { }
        @Override public void onEvent(int eventType, Bundle params) { }

        private final Handler uiHandler = new Handler(Looper.getMainLooper());

        // ユーザー発話 → メイン処理
        private void handleUserQuery(String userText) {
            LogUtils.logWithCaller(Thread.currentThread().getStackTrace(), "start");
            // 1) 日本語エリアに追加
//            uiHandler.post(() -> {
//                tvJapanese.append("▶ ユーザー: " + userText + "\n");
//            });

            // 2) chatHistory にユーザー発話を追加
//            JsonObject userMsg = new JsonObject();
//            userMsg.addProperty("role", "user");
//            userMsg.addProperty("content", userText);
//            chatHistory.add(userMsg);

            // 2) 英訳 (ユーザー発話) を取得して表示
            new Thread(() -> {
                String userEn = translateText(userText);
//                uiHandler.post(() -> {
//                    tvEnglish.append("▶ You: " + userEn + "\n");
//                });
                // 3) ChatGPT 応答 (日本語) を取得
                String replyJa = chatWithGPT(userText);
                uiHandler.post(() -> {
//                    tvJapanese.append("◀ AI: " + replyJa + "\n");
                    // --- ここから追加 ---
                    speakText(replyJa);
                    // --- ここまで追加 ---
                });
                // 4) 英訳 (AI応答) を取得して表示
                String replyEn = translateText(replyJa);
//                uiHandler.post(() -> {
//                    tvEnglish.append("◀ AI: " + replyEn + "\n");
//                });

                // 日本語ユーザー発話をDBに
                ChatMessage u = new ChatMessage();
                u.role = "user";
                u.contentJa = userText;
                u.contentEn = userEn;  // 翻訳結果
                u.timestamp = System.currentTimeMillis();
                viewModel.insert(u);

                // ChatGPT 応答後
                ChatMessage a = new ChatMessage();
                a.role = "assistant";
                a.contentJa = replyJa;
                a.contentEn = replyEn;
                a.timestamp = System.currentTimeMillis();
                viewModel.insert(a);
            }).start();
        }

        /** TTS で読み上げ */
        private void speakText(String text) {
            if (tts != null) {
                // QUEUE_FLUSH: 直前の発話をキャンセルして再生
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "UTTERANCE_ID");
            }
        }
        private String chatWithGPT(String userMessage) {
            LogUtils.logWithCaller(Thread.currentThread().getStackTrace(), "start");
            LogUtils.logWithCaller(Thread.currentThread().getStackTrace(), "chatHistory:" + String.valueOf(chatHistory));
            LogUtils.logWithCaller(Thread.currentThread().getStackTrace(), "temperature:" + String.valueOf(temperature));
            try {
                OkHttpClient client =  new OkHttpClient.Builder()
                        .connectTimeout(30, TimeUnit.SECONDS)   // 接続タイムアウト
                        .writeTimeout(30, TimeUnit.SECONDS)     // 書き込みタイムアウト
                        .readTimeout(60, TimeUnit.SECONDS)      // 読み取りタイムアウト
                        .build();
                // リクエストボディに chatHistory をセット
                JsonObject body = new JsonObject();
                body.addProperty("model", "gpt-4o");
                body.add("messages", chatHistory);
                // ② 温度を上げて人間らしさを演出
                body.addProperty("temperature", temperature);

                JsonArray msgs = new JsonArray();
                // system ロールで日本語会話を指定
                JsonObject sys = new JsonObject();
                sys.addProperty("role", "system");
                sys.addProperty("content", "あなたは日本語で会話するアシスタントです。");
                msgs.add(sys);
                // user ロール
                JsonObject usr = new JsonObject();
                usr.addProperty("role", "user");
                usr.addProperty("content", userMessage);
                msgs.add(usr);
                body.add("messages", msgs);

                Request request = new Request.Builder()
                        .url("https://api.openai.com/v1/chat/completions")
                        .addHeader("Authorization", "Bearer " + savedApiKey)
                        .post(RequestBody.create(
                                body.toString(),
                                MediaType.parse("application/json; charset=utf-8")
                        ))
                        .build();

                Response resp = client.newCall(request).execute();
                String json = resp.body().string();
                JsonObject jo = JsonParser.parseString(json).getAsJsonObject();
                String assistantText = jo
                        .getAsJsonArray("choices")
                        .get(0).getAsJsonObject()
                        .getAsJsonObject("message")
                        .get("content").getAsString().trim();

                // ■■ AI応答を chatHistory に追加 ■■
//                JsonObject assistantMsg = new JsonObject();
//                assistantMsg.addProperty("role", "assistant");
//                assistantMsg.addProperty("content", assistantText);
//                chatHistory.add(assistantMsg);

                return assistantText;
            } catch (SocketTimeoutException e) {
                Log.e("HomeFragment", "chatWithGPT timeout", e);
                return "[エラー] 通信がタイムアウトしました。後でもう一度お試しください。";
            } catch (Exception e) {
                e.printStackTrace();
                return "[Error]";
            }
        }

        private String translateText(String text) {
            LogUtils.logWithCaller(Thread.currentThread().getStackTrace(), "start");
            try {
                OkHttpClient client = new OkHttpClient();
                JsonObject body = new JsonObject();
                body.addProperty("model", "gpt-3.5-turbo");
                JsonArray msgs = new JsonArray();
                // 翻訳用 system ロール
                JsonObject sys = new JsonObject();
                sys.addProperty("role", "system");
                sys.addProperty("content", "以下の日本語を英語に翻訳してください。");
                msgs.add(sys);
                // 翻訳対象
                JsonObject usr = new JsonObject();
                usr.addProperty("role", "user");
                usr.addProperty("content", text);
                msgs.add(usr);
                body.add("messages", msgs);

                Request request = new Request.Builder()
                        .url("https://api.openai.com/v1/chat/completions")
                        .addHeader("Authorization", "Bearer " + savedApiKey)
                        .post(RequestBody.create(
                                body.toString(),
                                MediaType.parse("application/json; charset=utf-8")
                        ))
                        .build();

                Response resp = client.newCall(request).execute();
                String json = resp.body().string();
                JsonObject jo = JsonParser.parseString(json).getAsJsonObject();
                String en = jo
                        .getAsJsonArray("choices")
                        .get(0).getAsJsonObject()
                        .getAsJsonObject("message")
                        .get("content").getAsString();
                return en.trim();
            } catch (SocketTimeoutException e) {
                Log.e("HomeFragment", "chatWithGPT timeout", e);
                return "[エラー] 通信がタイムアウトしました。後でもう一度お試しください。";
            } catch (Exception e) {
                e.printStackTrace();
                return "[Error]";
            }
        }
    }
}