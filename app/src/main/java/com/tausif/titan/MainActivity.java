package com.tausif.titan;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Locale;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private TextToSpeech tts;
    private TextView txtOutput;
    private Button btnListen;
    private static final int REQ_CODE_SPEECH_INPUT = 100;
    private String userQuery = "";
    private String cleanQuery = "";
    private String commandType = "";
    private boolean sleepMode = false;
    private OkHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tts = new TextToSpeech(this, this);
        client = new OkHttpClient();
        txtOutput = findViewById(R.id.txtOutput);
        btnListen = findViewById(R.id.btnListen);

        btnListen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listen();
            }
        });
    }

    private void listen() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "बोलिए...");
        startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CODE_SPEECH_INPUT && resultCode == RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            userQuery = result.get(0);
            txtOutput.setText("आपने कहा: " + userQuery);
            processQuery(userQuery);
        }
    }

    private void processQuery(String query) {
        // Remove "titan" or "टाइटन"
        cleanQuery = query.replaceAll("(?i)(titan|टाइटन)\\s*", "").trim();
        txtOutput.append("\nClean Query: " + cleanQuery);

        if (sleepMode) {
            speak("मैं सो रहा हूँ। इमरजेंसी के लिए बोलें।");
            return;
        }

        if (cleanQuery.isEmpty()) {
            commandType = "welcome";
            handleWelcome();
            return;
        }

        if (cleanQuery.matches(".*\\b(kholo|open|start|launch|chalao)\\b.*")) {
            commandType = "app";
            handleApp();
            return;
        }

        if (cleanQuery.matches(".*\\b(alarm|reminder|call|message|sms|notification)\\b.*")) {
            commandType = "task";
            handleTask();
            return;
        }

        if (cleanQuery.matches(".*\\b(hi|hello|हैलो|नमस्ते|सलाम|assalamualaikum)\\b.*")) {
            commandType = "welcome";
            handleWelcome();
            return;
        }

        commandType = "chat";
        callAPI();
    }

    private void callAPI() {
        String url = "https://api.groq.com/openai/v1/chat/completions";
        String json = "{\"model\":\"llama-3.1-70b-versatile\",\"messages\":[{\"role\":\"system\",\"content\":\"You are Titan, Islamic AI assistant for Tausif Bhai. Respond in Hinglish. Max 2 sentences.\"},{\"role\":\"user\",\"content\":\"" + cleanQuery + "\"}],\"max_tokens\":200}";

        RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Authorization", "Bearer %GROQ_API_KEY%")
                .addHeader("Content-Type", "application/json")
                .build();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Response response = client.newCall(request).execute();
                    String responseBody = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    JSONArray choices = jsonResponse.getJSONArray("choices");
                    JSONObject firstChoice = choices.getJSONObject(0);
                    JSONObject message = firstChoice.getJSONObject("message");
                    final String reply = message.getString("content");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            txtOutput.append("\nTitan: " + reply);
                            speak(reply);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void handleWelcome() {
        speak("जी तौसीफ भाई, मैं हूँ टाइटन। बताइए क्या सेवा करनी है?");
    }

    private void handleApp() {
        speak("एप्लीकेशन खोल रहा हूँ।");
    }

    private void handleTask() {
        speak("टास्क प्रोसेस हो रहा है।");
    }

    private void speak(String text) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(new Locale("hi", "IN"));
        }
    }
}
