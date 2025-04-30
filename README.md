# TomaCh
ChatGPTと会話をするためのAndroidアプリケーション

# アプリ概要
本アプリは、Android 上で音声入力をトリガーに ChatGPT と会話し、そのやり取りを日本語⇔英語の対比表示で見ながら、会話履歴を永続化して再利用できるチャットクライアントです。  
以下の特徴を備えています。

---

## 主な機能
1. **音声入力**  
   - ボタン押下／ウェイクワード（「ジクサー」）で開始  
   - 端末の SpeechRecognizer を利用し、日本語認識

2. **ChatGPT API 呼び出し**  
   - ユーザー発話を gpt-4o（または gpt-3.5）に送信  
   - `temperature`（応答のクリエイティビティ）を UI 上のスライダーで動的設定  
   - レスポンスを日本語で受け取る

3. **英語翻訳**  
   - OpenAI の Chat API を使って日本語↔英語の翻訳  
   - 2カラムで日本語原文と英語訳を並列表示  
   - 「英訳表示」スイッチでオン／オフを切り替え、状態は SharedPreferences に永続化

4. **音声出力（TTS）**  
   - AI の応答を Android の TextToSpeech で読み上げ  
   - 「音声停止」ボタンで途中キャンセル

5. **履歴管理**  
   - 会話メッセージは Room データベースに保存  
   - アプリ起動時に過去ログを一括ロードし、RecyclerView で表示  
   - 個別メッセージの削除機能  
   - 履歴を起点に chatHistory（API 送信用 JSON 配列）を常に最新化
   

# 楽曲一覧画面
<img src="https://github.com/user-attachments/assets/b26c1b72-e2de-42c0-9f8a-709b9560edd4" width="300px">

<img src="https://github.com/user-attachments/assets/dbb2f107-8d77-4205-8a31-beffd9b5716f" width="300px">

<img src="https://github.com/user-attachments/assets/00bac54a-5c49-4671-86ec-edf7cf2fb72f" width="300px">

