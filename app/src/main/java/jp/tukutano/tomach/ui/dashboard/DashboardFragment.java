package jp.tukutano.tomach.ui.dashboard;

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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.Locale;

import jp.tukutano.tomach.R;
import jp.tukutano.tomach.databinding.FragmentDashboardBinding;
import jp.tukutano.tomach.util.LogUtils;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private EditText etApiKey;
    private Button btnSave;
    // プリファレンス名・キー
    private static final String PREFS_NAME = "openai_prefs";
    private static final String KEY_API = "api_key";
    private static final String KEY_SHOW_ENGLISH = "show_english";
    private SwitchCompat switchEnglish;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        LogUtils.logWithCaller(Thread.currentThread().getStackTrace(), "start");
        DashboardViewModel dashboardViewModel =
                new ViewModelProvider(this).get(DashboardViewModel.class);

        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // SharedPreferences 取得
        SharedPreferences settings = requireContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // 1) 保存済み設定を読み込んで初期状態をセット
        // スイッチ参照＆リスナー登録
        switchEnglish = root.findViewById(R.id.switchEnglish);
        boolean showEnglish = settings.getBoolean(KEY_SHOW_ENGLISH, true);
        switchEnglish.setChecked(showEnglish);


        // 2) スイッチ操作で設定を保存＆Adapterに反映
        switchEnglish.setOnCheckedChangeListener((button, isChecked) -> {
            // プリファレンスに永続化
            settings.edit()
                    .putBoolean(KEY_SHOW_ENGLISH, isChecked)
                    .apply();
            // Adapter 側に表示切替を通知
        });
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        etApiKey = view.findViewById(R.id.etApiKey);
        btnSave  = view.findViewById(R.id.btnSaveApiKey);

        // 既存のキーがあれば表示
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String saved = prefs.getString(KEY_API, "");
        etApiKey.setText(saved);

        btnSave.setOnClickListener(v -> {
            String key = etApiKey.getText().toString().trim();
            if (!key.isEmpty()) {
                prefs.edit()
                        .putString(KEY_API, key)
                        .apply();
                Toast.makeText(getContext(),
                        "APIキーを保存しました", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(),
                        "キーが空です", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}