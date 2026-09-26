package com.mitaoe.shridhar202401040197.util;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.ArrayList;
import java.util.Locale;

public class VoiceAssistantHelper {

    private static final String TAG = "VoiceAssistantHelper";

    public interface SpeechResultCallback {
        void onSpeechResult(String recognizedText);
        void onSpeechError(String errorMessage);
        void onSpeechRmsChanged(float rmsdB);
    }

    private final Context context;
    private SpeechRecognizer speechRecognizer;
    private TextToSpeech textToSpeech;
    private boolean isTtsInitialized = false;

    public VoiceAssistantHelper(Context context) {
        this.context = context.getApplicationContext();
        initTts();
    }

    private void initTts() {
        textToSpeech = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.getDefault());
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    textToSpeech.setLanguage(Locale.US);
                }
                isTtsInitialized = true;
            } else {
                Log.e(TAG, "TTS Initialization failed");
            }
        });
    }

    public void speak(String text) {
        if (textToSpeech != null && isTtsInitialized && text != null && !text.isEmpty()) {
            // Strip markdown asterisks and backticks for clean natural speech
            String cleanText = text.replaceAll("[*#`_~]", "");
            textToSpeech.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "UTTERANCE_ID");
        }
    }

    public void stopSpeaking() {
        if (textToSpeech != null && textToSpeech.isSpeaking()) {
            textToSpeech.stop();
        }
    }

    public void startListening(SpeechResultCallback callback) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            callback.onSpeechError("Speech recognition is not available on this device.");
            return;
        }

        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Listening to your assistant command...");

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {}

            @Override
            public void onBeginningOfSpeech() {}

            @Override
            public void onRmsChanged(float rmsdB) {
                callback.onSpeechRmsChanged(rmsdB);
            }

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {}

            @Override
            public void onError(int error) {
                String errorMsg = "Voice recognition error (" + error + ")";
                if (error == SpeechRecognizer.ERROR_NO_MATCH) {
                    errorMsg = "No speech detected. Please try again.";
                } else if (error == SpeechRecognizer.ERROR_NETWORK) {
                    errorMsg = "Network error during speech recognition.";
                }
                callback.onSpeechError(errorMsg);
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    callback.onSpeechResult(matches.get(0));
                } else {
                    callback.onSpeechError("No speech recognized.");
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {}

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });

        speechRecognizer.startListening(intent);
    }

    public void stopListening() {
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
        }
    }

    public void destroy() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
        }
    }
}
