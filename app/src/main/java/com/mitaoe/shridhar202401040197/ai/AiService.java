package com.mitaoe.shridhar202401040197.ai;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.mitaoe.shridhar202401040197.data.model.AiModel;
import com.mitaoe.shridhar202401040197.data.model.ChatMessage;
import com.mitaoe.shridhar202401040197.data.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class AiService {

    public interface StreamCallback {
        void onStart();
        void onToken(String token, String fullTextSoFar);
        void onComplete(String fullResponse);
        void onError(String errorMessage);
    }

    public interface ModelFetchCallback {
        void onSuccess(List<AiModel> models);
        void onError(String error);
    }

    private static final MediaType JSON_MEDIA = MediaType.get("application/json; charset=utf-8");
    private final PreferenceManager prefManager;
    private final OkHttpClient client;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newFixedThreadPool(3);

    public AiService(Context context) {
        this.prefManager = new PreferenceManager(context);
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public void generateResponse(List<ChatMessage> conversationHistory, String userPrompt, StreamCallback callback) {
        mainHandler.post(callback::onStart);

        executor.execute(() -> {
            String provider = prefManager.getActiveProvider();
            String modelId = prefManager.getActiveModelId();
            String apiKey = getApiKeyForProvider(provider);

            // If no key entered for cloud provider, use smart assistant mock response
            if (apiKey.isEmpty() && !"custom".equalsIgnoreCase(provider)) {
                runDemoAssistantResponse(userPrompt, modelId, callback);
                return;
            }

            try {
                if ("gemini".equalsIgnoreCase(provider) && !modelId.startsWith("http")) {
                    sendGeminiNativeRequest(apiKey, modelId, conversationHistory, userPrompt, callback);
                } else {
                    sendOpenAiCompatibleRequest(provider, apiKey, modelId, conversationHistory, userPrompt, callback);
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Network error: " + e.getMessage()));
            }
        });
    }

    private String getApiKeyForProvider(String provider) {
        switch (provider.toLowerCase()) {
            case "gemini": return prefManager.getGeminiApiKey();
            case "openrouter": return prefManager.getOpenRouterApiKey();
            case "groq": return prefManager.getGroqApiKey();
            case "nim": return prefManager.getNimApiKey();
            case "openai": return prefManager.getOpenAiApiKey();
            case "custom": return prefManager.getCustomApiKey();
            default: return "";
        }
    }

    private String getEndpointForProvider(String provider) {
        switch (provider.toLowerCase()) {
            case "openrouter": return "https://openrouter.ai/api/v1/chat/completions";
            case "groq": return "https://api.groq.com/openai/v1/chat/completions";
            case "nim": return "https://integrate.api.nvidia.com/v1/chat/completions";
            case "openai": return "https://api.openai.com/v1/chat/completions";
            case "gemini": return "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions";
            case "custom": return prefManager.getCustomBaseUrl() + (prefManager.getCustomBaseUrl().endsWith("/") ? "" : "/") + "chat/completions";
            default: return "https://openrouter.ai/api/v1/chat/completions";
        }
    }

    private void sendOpenAiCompatibleRequest(String provider, String apiKey, String modelId,
                                            List<ChatMessage> history, String userPrompt, StreamCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("model", modelId);
            body.put("stream", true);

            JSONArray messages = new JSONArray();
            // System prompt
            JSONObject sysMsg = new JSONObject();
            sysMsg.put("role", "system");
            sysMsg.put("content", "You are an intelligent, concise, and helpful personal AI assistant. You help manage tasks, notes, reminders, and answer queries cleanly.");
            messages.put(sysMsg);

            // History
            if (history != null) {
                int start = Math.max(0, history.size() - 8);
                for (int i = start; i < history.size(); i++) {
                    ChatMessage msg = history.get(i);
                    JSONObject chatObj = new JSONObject();
                    chatObj.put("role", msg.isUser() ? "user" : "assistant");
                    chatObj.put("content", msg.getText());
                    messages.put(chatObj);
                }
            }

            // Current prompt
            JSONObject currentMsg = new JSONObject();
            currentMsg.put("role", "user");
            currentMsg.put("content", userPrompt);
            messages.put(currentMsg);

            body.put("messages", messages);

            Request.Builder reqBuilder = new Request.Builder()
                    .url(getEndpointForProvider(provider))
                    .post(RequestBody.create(body.toString(), JSON_MEDIA))
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json");

            if ("openrouter".equalsIgnoreCase(provider)) {
                reqBuilder.addHeader("HTTP-Referer", "https://personalassistant.mitaoe.edu");
                reqBuilder.addHeader("X-Title", "AI Personal Assistant");
            }

            Response response = client.newCall(reqBuilder.build()).execute();
            if (!response.isSuccessful()) {
                String errBody = response.body() != null ? response.body().string() : "Error " + response.code();
                mainHandler.post(() -> callback.onError("Provider error (" + response.code() + "): " + errBody));
                return;
            }

            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                mainHandler.post(() -> callback.onError("Empty response from AI server."));
                return;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(responseBody.byteStream()));
            StringBuilder fullText = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith(":")) continue;

                if (line.startsWith("data:")) {
                    String data = line.substring(5).trim();
                    if ("[DONE]".equalsIgnoreCase(data)) break;

                    try {
                        JSONObject json = new JSONObject(data);
                        JSONArray choices = json.optJSONArray("choices");
                        if (choices != null && choices.length() > 0) {
                            JSONObject choice = choices.getJSONObject(0);
                            JSONObject delta = choice.optJSONObject("delta");
                            if (delta != null && delta.has("content")) {
                                String token = delta.getString("content");
                                fullText.append(token);
                                String current = fullText.toString();
                                mainHandler.post(() -> callback.onToken(token, current));
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }

            String finalResult = fullText.toString();
            mainHandler.post(() -> callback.onComplete(finalResult));

        } catch (Exception e) {
            mainHandler.post(() -> callback.onError("Request error: " + e.getMessage()));
        }
    }

    private void sendGeminiNativeRequest(String apiKey, String modelId,
                                        List<ChatMessage> history, String userPrompt, StreamCallback callback) {
        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/" + modelId + ":generateContent?key=" + apiKey;

            JSONObject root = new JSONObject();
            JSONArray contents = new JSONArray();

            JSONObject userPart = new JSONObject();
            userPart.put("text", userPrompt);
            JSONArray partsArray = new JSONArray();
            partsArray.put(userPart);

            JSONObject contentObj = new JSONObject();
            contentObj.put("role", "user");
            contentObj.put("parts", partsArray);
            contents.put(contentObj);

            root.put("contents", contents);

            Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(root.toString(), JSON_MEDIA))
                    .build();

            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                String err = response.body() != null ? response.body().string() : "Error " + response.code();
                mainHandler.post(() -> callback.onError("Gemini API Error (" + response.code() + "): " + err));
                return;
            }

            String responseString = response.body() != null ? response.body().string() : "";
            JSONObject resJson = new JSONObject(responseString);
            JSONArray candidates = resJson.optJSONArray("candidates");
            if (candidates != null && candidates.length() > 0) {
                JSONObject first = candidates.getJSONObject(0);
                JSONObject content = first.getJSONObject("content");
                JSONArray parts = content.getJSONArray("parts");
                String reply = parts.getJSONObject(0).getString("text");

                // Simulate typewriter stream for consistency
                simulateTypewriter(reply, callback);
            } else {
                mainHandler.post(() -> callback.onError("No candidates returned by Gemini."));
            }

        } catch (Exception e) {
            mainHandler.post(() -> callback.onError("Gemini Error: " + e.getMessage()));
        }
    }

    private void runDemoAssistantResponse(String userPrompt, String modelId, StreamCallback callback) {
        String answer;
        String lower = userPrompt.toLowerCase();

        if (lower.contains("hello") || lower.contains("hi") || lower.contains("who are you")) {
            answer = "Hello! I am your **AI Personal Assistant**.\n\nI can help you with:\n- ⏰ Scheduling tasks and reminders with alerts\n- 📝 Taking, organizing, and summarizing notes\n- 💡 Answering your queries with free AI models\n\n*(Note: You are currently running in **Demo Mode**. You can connect your free OpenRouter or Gemini API key in **Settings ⚙️** at any time!)*";
        } else if (lower.contains("task") || lower.contains("todo")) {
            answer = "You can manage your tasks easily! Just tell me things like:\n- *\"Add task: prepare project submission with high priority\"*\n- *\"Remind me to call John tomorrow at 5 PM\"*\n\nI will automatically parse the date, time, and priority, and alert you with an alarm!";
        } else if (lower.contains("note")) {
            answer = "I can keep track of all your notes! Try saying:\n- *\"Note: Grocery list for the week\"*\n- Or open the **Notes** tab to generate AI summaries and action items.";
        } else {
            answer = "Here is what I found for you:\n\n**" + userPrompt + "**\n\nTo unlock live internet reasoning with **" + modelId + "**, tap the **Settings (⚙️)** icon in the top right to paste your free OpenRouter, Gemini, or Groq API key!";
        }

        simulateTypewriter(answer, callback);
    }

    private void simulateTypewriter(String text, StreamCallback callback) {
        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < words.length; i++) {
            current.append(words[i]).append(" ");
            final String token = words[i] + " ";
            final String textSoFar = current.toString();
            mainHandler.post(() -> callback.onToken(token, textSoFar));
            try {
                Thread.sleep(25);
            } catch (InterruptedException ignored) {}
        }

        final String finalResult = current.toString().trim();
        mainHandler.post(() -> callback.onComplete(finalResult));
    }

    public void fetchAvailableModels(String provider, String apiKey, ModelFetchCallback callback) {
        executor.execute(() -> {
            try {
                String url;
                if ("groq".equalsIgnoreCase(provider)) {
                    url = "https://api.groq.com/openai/v1/models";
                } else if ("openrouter".equalsIgnoreCase(provider)) {
                    url = "https://openrouter.ai/api/v1/models";
                } else if ("openai".equalsIgnoreCase(provider)) {
                    url = "https://api.openai.com/v1/models";
                } else {
                    mainHandler.post(() -> callback.onError("Live model fetching is supported for OpenRouter, Groq, and OpenAI."));
                    return;
                }

                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .addHeader("Authorization", "Bearer " + apiKey)
                        .build();

                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) {
                    mainHandler.post(() -> callback.onError("Fetch error (" + response.code() + ")"));
                    return;
                }

                String json = response.body() != null ? response.body().string() : "";
                JSONObject obj = new JSONObject(json);
                JSONArray data = obj.optJSONArray("data");

                List<AiModel> result = new ArrayList<>();
                if (data != null) {
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject m = data.getJSONObject(i);
                        String id = m.getString("id");
                        String name = m.optString("name", id);
                        boolean isFree = id.contains(":free") || id.contains("free") || "groq".equalsIgnoreCase(provider);
                        String desc = isFree ? "Free tier model" : "Standard model";
                        result.add(new AiModel(id, name, provider, isFree, false, desc));
                    }
                }

                mainHandler.post(() -> callback.onSuccess(result));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Error: " + e.getMessage()));
            }
        });
    }
}
