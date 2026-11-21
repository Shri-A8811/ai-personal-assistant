package com.mitaoe.shridhar202401040197.ai;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import com.mitaoe.shridhar202401040197.data.model.AiModel;
import com.mitaoe.shridhar202401040197.data.model.ChatMessage;
import com.mitaoe.shridhar202401040197.data.preference.PreferenceManager;
import com.mitaoe.shridhar202401040197.util.AppExecutors;
import com.mitaoe.shridhar202401040197.util.ImageUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
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

    public static class RequestHandle {
        private final java.util.concurrent.atomic.AtomicBoolean cancelled = new java.util.concurrent.atomic.AtomicBoolean(false);
        private volatile okhttp3.Call activeCall;

        public void bindCall(okhttp3.Call call) {
            this.activeCall = call;
            if (cancelled.get() && call != null) {
                call.cancel();
            }
        }

        public void cancel() {
            cancelled.set(true);
            okhttp3.Call c = activeCall;
            if (c != null) {
                c.cancel();
            }
        }

        public boolean isCancelled() {
            return cancelled.get();
        }
    }

    private static final MediaType JSON_MEDIA = MediaType.get("application/json; charset=utf-8");
    private static final OkHttpClient SHARED_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    private final Context context;
    private final PreferenceManager prefManager;
    private final OkHttpClient client = SHARED_CLIENT;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public AiService(Context context) {
        this.context = context.getApplicationContext();
        this.prefManager = new PreferenceManager(context);
    }

    public RequestHandle generateResponse(List<ChatMessage> conversationHistory, String userPrompt, StreamCallback callback) {
        return generateResponse(conversationHistory, userPrompt, null, callback);
    }

    public RequestHandle generateResponse(List<ChatMessage> conversationHistory, String userPrompt, String imageUriStr, StreamCallback callback) {
        final RequestHandle handle = new RequestHandle();
        mainHandler.post(() -> {
            if (!handle.isCancelled()) callback.onStart();
        });

        AppExecutors.getInstance().networkIO().execute(() -> {
            if (handle.isCancelled()) return;

            String provider = prefManager.getActiveProvider();
            String modelId = prefManager.getActiveModelId();

            if (provider == null || provider.isEmpty() || modelId == null || modelId.isEmpty()) {
                mainHandler.post(() -> {
                    if (!handle.isCancelled()) callback.onError("No AI model selected. Please open Settings to configure your API key and pin models.");
                });
                return;
            }

            String apiKey = prefManager.getApiKey(provider);
            if (apiKey.isEmpty() && !"custom".equalsIgnoreCase(provider)) {
                mainHandler.post(() -> {
                    if (!handle.isCancelled()) callback.onError("No API key configured for " + provider.toUpperCase() + ".\nPlease open Settings to enter your key.");
                });
                return;
            }

            ImageUtil.EncodedImage encodedImage = null;
            if (imageUriStr != null && !imageUriStr.isEmpty()) {
                try {
                    encodedImage = ImageUtil.encodeUriToBase64(context, Uri.parse(imageUriStr));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (handle.isCancelled()) return;

            try {
                if ("anthropic".equalsIgnoreCase(provider)) {
                    sendAnthropicRequest(apiKey, modelId, conversationHistory, userPrompt, encodedImage, handle, callback);
                } else if ("gemini".equalsIgnoreCase(provider)) {
                    sendGeminiNativeRequest(apiKey, modelId, conversationHistory, userPrompt, encodedImage, handle, callback);
                } else {
                    sendOpenAiCompatibleRequest(provider, apiKey, modelId, conversationHistory, userPrompt, encodedImage, handle, callback);
                }
            } catch (Exception e) {
                if (!handle.isCancelled()) {
                    mainHandler.post(() -> callback.onError("Network error: " + e.getMessage()));
                }
            }
        });
        return handle;
    }

    private String getEndpointForProvider(String provider) {
        switch (provider.toLowerCase()) {
            case "openrouter": return "https://openrouter.ai/api/v1/chat/completions";
            case "groq": return "https://api.groq.com/openai/v1/chat/completions";
            case "deepseek": return "https://api.deepseek.com/chat/completions";
            case "xai": return "https://api.x.ai/v1/chat/completions";
            case "openai": return "https://api.openai.com/v1/chat/completions";
            case "fireworks": return "https://api.fireworks.ai/inference/v1/chat/completions";
            case "gemini": return "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions";
            case "custom":
                String customUrl = prefManager.getCustomBaseUrl();
                if (customUrl == null) return "";
                customUrl = customUrl.trim();
                if (!customUrl.startsWith("http://") && !customUrl.startsWith("https://")) return "";
                if (!customUrl.endsWith("/")) customUrl += "/";
                return customUrl + "chat/completions";
            default: return "https://openrouter.ai/api/v1/chat/completions";
        }
    }

    private void sendOpenAiCompatibleRequest(String provider, String apiKey, String modelId,
                                            List<ChatMessage> history, String userPrompt,
                                            ImageUtil.EncodedImage encodedImage, RequestHandle handle, StreamCallback callback) {
        String endpoint = getEndpointForProvider(provider);
        if (endpoint.isEmpty()) {
            mainHandler.post(() -> {
                if (!handle.isCancelled()) {
                    callback.onError("Invalid custom base URL. Set a valid http:// or https:// URL in Settings.");
                }
            });
            return;
        }
        try {
            JSONObject body = new JSONObject();
            body.put("model", modelId);
            body.put("stream", true);

            JSONArray messages = new JSONArray();
            // System prompt
            JSONObject sysMsg = new JSONObject();
            sysMsg.put("role", "system");
            sysMsg.put("content", getFullSystemPrompt());
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
            if (encodedImage != null) {
                JSONArray parts = new JSONArray();
                JSONObject textPart = new JSONObject();
                textPart.put("type", "text");
                textPart.put("text", (userPrompt != null && !userPrompt.trim().isEmpty()) ? userPrompt : "Analyze and describe this image.");
                parts.put(textPart);

                JSONObject imagePart = new JSONObject();
                imagePart.put("type", "image_url");
                JSONObject imageUrlObj = new JSONObject();
                imageUrlObj.put("url", "data:" + encodedImage.mimeType + ";base64," + encodedImage.base64);
                imagePart.put("image_url", imageUrlObj);
                parts.put(imagePart);

                currentMsg.put("content", parts);
            } else {
                currentMsg.put("content", userPrompt != null ? userPrompt : "");
            }
            messages.put(currentMsg);

            body.put("messages", messages);

            Request.Builder reqBuilder = new Request.Builder()
                    .url(endpoint)
                    .post(RequestBody.create(body.toString(), JSON_MEDIA))
                    .addHeader("Content-Type", "application/json");

            if (!apiKey.isEmpty()) {
                reqBuilder.addHeader("Authorization", "Bearer " + apiKey);
            }

            if ("openrouter".equalsIgnoreCase(provider)) {
                reqBuilder.addHeader("HTTP-Referer", "https://personalassistant.mitaoe.edu");
                reqBuilder.addHeader("X-Title", "AI Personal Assistant");
            }

            okhttp3.Call call = client.newCall(reqBuilder.build());
            handle.bindCall(call);
            try (Response response = call.execute()) {
                if (!response.isSuccessful()) {
                    String errBody = response.body() != null ? response.body().string() : "Error " + response.code();
                    if (!handle.isCancelled()) {
                        mainHandler.post(() -> callback.onError(provider.toUpperCase() + " error (" + response.code() + "): " + errBody));
                    }
                    return;
                }

                ResponseBody responseBody = response.body();
                if (responseBody == null) {
                    if (!handle.isCancelled()) {
                        mainHandler.post(() -> callback.onError("Empty response from AI server."));
                    }
                    return;
                }

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(responseBody.byteStream()))) {
                    StringBuilder fullText = new StringBuilder();
                    String line;
                    long lastDispatchMs = 0L;

                    while (!handle.isCancelled() && (line = reader.readLine()) != null) {
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
                                    if (delta != null && !delta.isNull("content")) {
                                        String token = delta.optString("content", "");
                                        if (!token.isEmpty()) {
                                            fullText.append(token);
                                            long now = android.os.SystemClock.uptimeMillis();
                                            if (now - lastDispatchMs >= 45L) {
                                                lastDispatchMs = now;
                                                final String current = fullText.toString();
                                                mainHandler.post(() -> {
                                                    if (!handle.isCancelled()) callback.onToken(token, current);
                                                });
                                            }
                                        }
                                    }
                                }
                            } catch (Exception ignored) {}
                        }
                    }

                    if (handle.isCancelled()) {
                        return;
                    }

                    String finalResult = fullText.toString();
                    if (finalResult.trim().isEmpty()) {
                        mainHandler.post(() -> callback.onError("The model returned an empty response. Try again or switch models."));
                    } else {
                        mainHandler.post(() -> callback.onComplete(finalResult));
                    }
                }
            }

        } catch (Exception e) {
            if (!handle.isCancelled()) {
                mainHandler.post(() -> callback.onError("Request error: " + e.getMessage()));
            }
        }
    }

    private void sendAnthropicRequest(String apiKey, String modelId,
                                      List<ChatMessage> history, String userPrompt,
                                      ImageUtil.EncodedImage encodedImage, RequestHandle handle, StreamCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("model", modelId);
            body.put("max_tokens", 2048);
            body.put("system", getFullSystemPrompt());

            JSONArray messages = new JSONArray();
            if (history != null) {
                int start = Math.max(0, history.size() - 6);
                for (int i = start; i < history.size(); i++) {
                    ChatMessage msg = history.get(i);
                    JSONObject chatObj = new JSONObject();
                    chatObj.put("role", msg.isUser() ? "user" : "assistant");
                    chatObj.put("content", msg.getText());
                    messages.put(chatObj);
                }
            }
            JSONObject currentMsg = new JSONObject();
            currentMsg.put("role", "user");
            if (encodedImage != null) {
                JSONArray contentArray = new JSONArray();
                JSONObject imgPart = new JSONObject();
                imgPart.put("type", "image");
                JSONObject source = new JSONObject();
                source.put("type", "base64");
                source.put("media_type", encodedImage.mimeType);
                source.put("data", encodedImage.base64);
                imgPart.put("source", source);
                contentArray.put(imgPart);

                JSONObject textPart = new JSONObject();
                textPart.put("type", "text");
                textPart.put("text", (userPrompt != null && !userPrompt.trim().isEmpty()) ? userPrompt : "Analyze and describe this image.");
                contentArray.put(textPart);

                currentMsg.put("content", contentArray);
            } else {
                currentMsg.put("content", userPrompt != null ? userPrompt : "");
            }
            messages.put(currentMsg);

            body.put("messages", messages);

            Request request = new Request.Builder()
                    .url("https://api.anthropic.com/v1/messages")
                    .post(RequestBody.create(body.toString(), JSON_MEDIA))
                    .addHeader("x-api-key", apiKey)
                    .addHeader("anthropic-version", "2023-06-01")
                    .addHeader("content-type", "application/json")
                    .build();

            okhttp3.Call call = client.newCall(request);
            handle.bindCall(call);
            try (Response response = call.execute()) {
                if (!response.isSuccessful()) {
                    String err = response.body() != null ? response.body().string() : "Error " + response.code();
                    if (!handle.isCancelled()) {
                        mainHandler.post(() -> callback.onError("Anthropic error (" + response.code() + "): " + err));
                    }
                    return;
                }

                String responseString = response.body() != null ? response.body().string() : "";
                JSONObject resJson = new JSONObject(responseString);
                JSONArray content = resJson.optJSONArray("content");
                if (content != null && content.length() > 0) {
                    String reply = content.getJSONObject(0).optString("text", "");
                    if (reply.trim().isEmpty()) {
                        if (!handle.isCancelled()) mainHandler.post(() -> callback.onError("Anthropic returned an empty message."));
                    } else {
                        simulateTypewriter(reply, handle, callback);
                    }
                } else if (!handle.isCancelled()) {
                    mainHandler.post(() -> callback.onError("No response text from Anthropic."));
                }
            }

        } catch (Exception e) {
            if (!handle.isCancelled()) {
                mainHandler.post(() -> callback.onError("Anthropic error: " + e.getMessage()));
            }
        }
    }

    private void sendGeminiNativeRequest(String apiKey, String modelId,
                                        List<ChatMessage> history, String userPrompt,
                                        ImageUtil.EncodedImage encodedImage, RequestHandle handle, StreamCallback callback) {
        try {
            String cleanModel = modelId.startsWith("models/") ? modelId.substring(7) : modelId;
            String url = "https://generativelanguage.googleapis.com/v1beta/models/" + cleanModel + ":generateContent";

            JSONObject root = new JSONObject();
            JSONArray contents = new JSONArray();

            // Inject multi-turn conversation history turns
            if (history != null) {
                int start = Math.max(0, history.size() - 8);
                for (int i = start; i < history.size(); i++) {
                    ChatMessage msg = history.get(i);
                    JSONObject turnObj = new JSONObject();
                    turnObj.put("role", msg.isUser() ? "user" : "model");
                    JSONArray turnParts = new JSONArray();
                    JSONObject turnTextPart = new JSONObject();
                    turnTextPart.put("text", msg.getText() != null ? msg.getText() : "");
                    turnParts.put(turnTextPart);
                    turnObj.put("parts", turnParts);
                    contents.put(turnObj);
                }
            }

            // Current prompt turn
            JSONArray partsArray = new JSONArray();
            if (encodedImage != null) {
                JSONObject imgPart = new JSONObject();
                JSONObject inlineData = new JSONObject();
                inlineData.put("mime_type", encodedImage.mimeType);
                inlineData.put("data", encodedImage.base64);
                imgPart.put("inline_data", inlineData);
                partsArray.put(imgPart);
            }

            JSONObject userPart = new JSONObject();
            userPart.put("text", (userPrompt != null && !userPrompt.trim().isEmpty()) ? userPrompt : "Analyze and describe this image.");
            partsArray.put(userPart);

            JSONObject contentObj = new JSONObject();
            contentObj.put("role", "user");
            contentObj.put("parts", partsArray);
            contents.put(contentObj);

            root.put("contents", contents);

            String systemInstruction = getFullSystemPrompt();
            if (systemInstruction != null && !systemInstruction.trim().isEmpty()) {
                JSONObject sysObj = new JSONObject();
                JSONArray sysParts = new JSONArray();
                JSONObject sysTextPart = new JSONObject();
                sysTextPart.put("text", systemInstruction);
                sysParts.put(sysTextPart);
                sysObj.put("parts", sysParts);
                root.put("system_instruction", sysObj);
            }

            Request.Builder reqBuilder = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(root.toString(), JSON_MEDIA));

            if (apiKey != null && !apiKey.isEmpty()) {
                reqBuilder.addHeader("x-goog-api-key", apiKey);
            }

            okhttp3.Call call = client.newCall(reqBuilder.build());
            handle.bindCall(call);
            try (Response response = call.execute()) {
                if (!response.isSuccessful()) {
                    String err = response.body() != null ? response.body().string() : "Error " + response.code();
                    if (!handle.isCancelled()) {
                        mainHandler.post(() -> callback.onError("Gemini API Error (" + response.code() + "): " + err));
                    }
                    return;
                }

                String responseString = response.body() != null ? response.body().string() : "";
                JSONObject resJson = new JSONObject(responseString);
                JSONArray candidates = resJson.optJSONArray("candidates");
                if (candidates != null && candidates.length() > 0) {
                    JSONObject first = candidates.optJSONObject(0);
                    JSONObject content = first != null ? first.optJSONObject("content") : null;
                    JSONArray parts = content != null ? content.optJSONArray("parts") : null;
                    String reply = (parts != null && parts.length() > 0 && parts.optJSONObject(0) != null)
                            ? parts.optJSONObject(0).optString("text", "")
                            : "";

                    if (!reply.isEmpty()) {
                        simulateTypewriter(reply, handle, callback);
                    } else if (!handle.isCancelled()) {
                        String reason = first != null ? first.optString("finishReason", "UNKNOWN") : "UNKNOWN";
                        mainHandler.post(() -> callback.onError("Gemini did not return text (reason: " + reason + ")."));
                    }
                } else if (!handle.isCancelled()) {
                    mainHandler.post(() -> callback.onError("No candidates returned by Gemini."));
                }
            }

        } catch (Exception e) {
            if (!handle.isCancelled()) {
                mainHandler.post(() -> callback.onError("Gemini Error: " + e.getMessage()));
            }
        }
    }

    private void simulateTypewriter(String text, RequestHandle handle, StreamCallback callback) {
        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();
        int chunkSize = 3;

        for (int i = 0; i < words.length; i += chunkSize) {
            if (handle.isCancelled()) return;
            StringBuilder chunk = new StringBuilder();
            for (int j = i; j < Math.min(words.length, i + chunkSize); j++) {
                chunk.append(words[j]).append(" ");
            }
            String token = chunk.toString();
            current.append(token);
            final String textSoFar = current.toString();
            mainHandler.post(() -> {
                if (!handle.isCancelled()) callback.onToken(token, textSoFar);
            });
            try {
                Thread.sleep(35);
            } catch (InterruptedException ignored) {
                return;
            }
        }

        if (!handle.isCancelled()) {
            final String finalResult = current.toString().trim();
            mainHandler.post(() -> {
                if (!handle.isCancelled()) callback.onComplete(finalResult);
            });
        }
    }

    public void fetchAvailableModels(String provider, String apiKey, ModelFetchCallback callback) {
        AppExecutors.getInstance().networkIO().execute(() -> {
            try {
                String pKey = provider.toLowerCase();
                List<AiModel> result = new ArrayList<>();

                if ("openrouter".equals(pKey)) {
                    String url = "https://openrouter.ai/api/v1/models";
                    Request.Builder rb = new Request.Builder().url(url).get();
                    if (!apiKey.isEmpty()) rb.addHeader("Authorization", "Bearer " + apiKey);

                    try (Response response = client.newCall(rb.build()).execute()) {
                        if (!response.isSuccessful()) {
                            mainHandler.post(() -> callback.onError("OpenRouter fetch error (" + response.code() + ")"));
                            return;
                        }
                        String json = response.body() != null ? response.body().string() : "";
                        JSONObject obj = new JSONObject(json);
                        JSONArray data = obj.optJSONArray("data");
                        if (data != null) {
                            for (int i = 0; i < data.length(); i++) {
                                JSONObject m = data.getJSONObject(i);
                                String id = m.getString("id");
                                String name = m.optString("name", id);
                                JSONObject pricing = m.optJSONObject("pricing");
                                boolean isFree = id.contains(":free") || id.toLowerCase().contains("free");
                                if (pricing != null) {
                                    String promptCost = pricing.optString("prompt", "1");
                                    if ("0".equals(promptCost) || "0.0".equals(promptCost)) isFree = true;
                                }
                                String desc = isFree ? "Free OpenRouter Model" : "OpenRouter Cloud";
                                result.add(new AiModel(id, name, "openrouter", isFree, false, desc));
                            }
                        }
                    }
                } else if ("groq".equals(pKey)) {
                    String url = "https://api.groq.com/openai/v1/models";
                    Request request = new Request.Builder()
                            .url(url)
                            .get()
                            .addHeader("Authorization", "Bearer " + apiKey)
                            .build();

                    try (Response response = client.newCall(request).execute()) {
                        if (!response.isSuccessful()) {
                            mainHandler.post(() -> callback.onError("Groq fetch error (" + response.code() + ")"));
                            return;
                        }
                        String json = response.body() != null ? response.body().string() : "";
                        JSONObject obj = new JSONObject(json);
                        JSONArray data = obj.optJSONArray("data");
                        if (data != null) {
                            for (int i = 0; i < data.length(); i++) {
                                JSONObject m = data.getJSONObject(i);
                                String id = m.getString("id");
                                result.add(new AiModel(id, id, "groq", true, false, "Ultra-fast Groq LPU"));
                            }
                        }
                    }
                } else if ("gemini".equals(pKey)) {
                    String url = "https://generativelanguage.googleapis.com/v1beta/models";
                    Request.Builder reqBuilder = new Request.Builder().url(url).get();
                    if (apiKey != null && !apiKey.isEmpty()) {
                        reqBuilder.addHeader("x-goog-api-key", apiKey);
                    }

                    try (Response response = client.newCall(reqBuilder.build()).execute()) {
                        if (!response.isSuccessful()) {
                            mainHandler.post(() -> callback.onError("Gemini fetch error (" + response.code() + ")"));
                            return;
                        }
                        String json = response.body() != null ? response.body().string() : "";
                        JSONObject obj = new JSONObject(json);
                        JSONArray models = obj.optJSONArray("models");
                        if (models != null) {
                            for (int i = 0; i < models.length(); i++) {
                                JSONObject m = models.getJSONObject(i);
                                String rawName = m.getString("name");
                                String id = rawName.startsWith("models/") ? rawName.substring(7) : rawName;
                                String dispName = m.optString("displayName", id);
                                String desc = m.optString("description", "Google AI Model");
                                // Filter only generateContent models
                                JSONArray methods = m.optJSONArray("supportedGenerationMethods");
                                boolean canGen = false;
                                if (methods != null) {
                                    for (int j = 0; j < methods.length(); j++) {
                                        if ("generateContent".equalsIgnoreCase(methods.getString(j))) {
                                            canGen = true;
                                            break;
                                        }
                                    }
                                }
                                if (canGen) {
                                    boolean isFree = id.contains("flash") || id.contains("exp");
                                    result.add(new AiModel(id, dispName, "gemini", isFree, false, desc));
                                }
                            }
                        }
                    }
                } else if ("deepseek".equals(pKey)) {
                    String url = "https://api.deepseek.com/models";
                    Request request = new Request.Builder()
                            .url(url)
                            .get()
                            .addHeader("Authorization", "Bearer " + apiKey)
                            .build();

                    try (Response response = client.newCall(request).execute()) {
                        if (response.isSuccessful()) {
                            String json = response.body() != null ? response.body().string() : "";
                            JSONObject obj = new JSONObject(json);
                            JSONArray data = obj.optJSONArray("data");
                            if (data != null && data.length() > 0) {
                                for (int i = 0; i < data.length(); i++) {
                                    JSONObject m = data.getJSONObject(i);
                                    String id = m.getString("id");
                                    result.add(new AiModel(id, id.equalsIgnoreCase("deepseek-chat") ? "DeepSeek V3 (Chat)" : (id.equalsIgnoreCase("deepseek-reasoner") ? "DeepSeek R1 (Reasoner)" : id), "deepseek", false, false, "DeepSeek Intelligence"));
                                }
                            }
                        } else {
                            // Fallback flagship models
                            result.add(new AiModel("deepseek-chat", "DeepSeek V3 (Chat)", "deepseek", false, false, "DeepSeek flagship general model"));
                            result.add(new AiModel("deepseek-reasoner", "DeepSeek R1 (Reasoner)", "deepseek", false, false, "DeepSeek advanced reasoning model"));
                        }
                    }
                } else if ("anthropic".equals(pKey)) {
                    result.add(new AiModel("claude-3-5-sonnet-20241022", "Claude 3.5 Sonnet", "anthropic", false, false, "Flagship Anthropic model"));
                    result.add(new AiModel("claude-3-5-haiku-20241022", "Claude 3.5 Haiku", "anthropic", false, false, "Ultra-fast lightweight Claude"));
                    result.add(new AiModel("claude-3-opus-20240229", "Claude 3 Opus", "anthropic", false, false, "Deep complex reasoning"));
                } else if ("xai".equals(pKey)) {
                    result.add(new AiModel("grok-2-1212", "Grok 2", "xai", false, false, "xAI flagship Grok intelligence"));
                    result.add(new AiModel("grok-2-vision-1212", "Grok 2 Vision", "xai", false, false, "Multimodal visual reasoning"));
                    result.add(new AiModel("grok-beta", "Grok Beta", "xai", false, false, "Latest experimental Grok"));
                } else if ("fireworks".equals(pKey)) {
                    String url = "https://api.fireworks.ai/inference/v1/models";
                    Request request = new Request.Builder()
                            .url(url)
                            .get()
                            .addHeader("Authorization", "Bearer " + apiKey)
                            .build();

                    try (Response response = client.newCall(request).execute()) {
                        if (!response.isSuccessful()) {
                            mainHandler.post(() -> callback.onError("Fireworks fetch error (" + response.code() + ")"));
                            return;
                        }
                        String json = response.body() != null ? response.body().string() : "";
                        JSONObject obj = new JSONObject(json);
                        JSONArray data = obj.optJSONArray("data");
                        if (data != null) {
                            for (int i = 0; i < data.length(); i++) {
                                JSONObject m = data.getJSONObject(i);
                                String id = m.getString("id");
                                result.add(new AiModel(id, id, "fireworks", false, false, "Fireworks Fast Inference"));
                            }
                        }
                    }
                } else if ("openai".equals(pKey)) {
                    String url = "https://api.openai.com/v1/models";
                    Request request = new Request.Builder()
                            .url(url)
                            .get()
                            .addHeader("Authorization", "Bearer " + apiKey)
                            .build();

                    try (Response response = client.newCall(request).execute()) {
                        if (!response.isSuccessful()) {
                            mainHandler.post(() -> callback.onError("OpenAI fetch error (" + response.code() + ")"));
                            return;
                        }
                        String json = response.body() != null ? response.body().string() : "";
                        JSONObject obj = new JSONObject(json);
                        JSONArray data = obj.optJSONArray("data");
                        if (data != null) {
                            for (int i = 0; i < data.length(); i++) {
                                JSONObject m = data.getJSONObject(i);
                                String id = m.getString("id");
                                if (id.startsWith("gpt-") || id.startsWith("o1")) {
                                    result.add(new AiModel(id, id, "openai", false, false, "OpenAI ChatGPT model"));
                                }
                            }
                        }
                    }
                } else if ("custom".equals(pKey)) {
                    String customUrl = prefManager.getCustomBaseUrl();
                    if (customUrl == null || (!customUrl.trim().startsWith("http://") && !customUrl.trim().startsWith("https://"))) {
                        mainHandler.post(() -> callback.onError("Invalid custom base URL."));
                        return;
                    }
                    customUrl = customUrl.trim();
                    if (!customUrl.endsWith("/")) customUrl += "/";
                    String url = customUrl + "models";
                    Request.Builder rb = new Request.Builder().url(url).get();
                    if (!apiKey.isEmpty()) rb.addHeader("Authorization", "Bearer " + apiKey);

                    try (Response response = client.newCall(rb.build()).execute()) {
                        if (response.isSuccessful()) {
                            String json = response.body() != null ? response.body().string() : "";
                            JSONObject obj = new JSONObject(json);
                            JSONArray data = obj.optJSONArray("data");
                            if (data != null) {
                                for (int i = 0; i < data.length(); i++) {
                                    JSONObject m = data.getJSONObject(i);
                                    String id = m.getString("id");
                                    result.add(new AiModel(id, id, "custom", true, false, "Local / Custom Endpoint"));
                                }
                            }
                        } else {
                            mainHandler.post(() -> callback.onError("Custom endpoint error (" + response.code() + ")"));
                            return;
                        }
                    }
                }

                // Cache the fetched models in background
                prefManager.mergeCachedFetchedModels(result);

                mainHandler.post(() -> callback.onSuccess(result));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Error: " + e.getMessage()));
            }
        });
    }

    private String getFullSystemPrompt() {
        String basePrompt = prefManager.getEffectiveSystemPrompt();
        if (prefManager.isChatWithNotesEnabled()) {
            try {
                List<com.mitaoe.shridhar202401040197.data.model.NoteItem> notes = 
                    com.mitaoe.shridhar202401040197.data.database.AppDatabase.getInstance(context).noteDao().getAllNotesSync();
                if (notes != null && !notes.isEmpty()) {
                    StringBuilder sb = new StringBuilder("\n\n[User's Saved Notes for Reference]:\n");
                    int count = Math.min(notes.size(), 8);
                    for (int i = 0; i < count; i++) {
                        com.mitaoe.shridhar202401040197.data.model.NoteItem note = notes.get(i);
                        sb.append("• ").append(note.getTitle()).append(": ").append(note.getContent()).append("\n");
                    }
                    return basePrompt + sb.toString();
                }
            } catch (Exception ignored) {}
        }
        return basePrompt;
    }
}
