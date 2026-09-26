package com.mitaoe.shridhar202401040197.data.preference;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mitaoe.shridhar202401040197.data.model.AiModel;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class PreferenceManager {

    private static final String PREF_NAME = "ai_assistant_prefs";
    private static final String KEY_GEMINI_API_KEY = "gemini_api_key";
    private static final String KEY_OPENROUTER_API_KEY = "openrouter_api_key";
    private static final String KEY_GROQ_API_KEY = "groq_api_key";
    private static final String KEY_NIM_API_KEY = "nim_api_key";
    private static final String KEY_OPENAI_API_KEY = "openai_api_key";
    private static final String KEY_CUSTOM_URL = "custom_base_url";
    private static final String KEY_CUSTOM_KEY = "custom_api_key";

    private static final String KEY_ACTIVE_PROVIDER = "active_provider";
    private static final String KEY_ACTIVE_MODEL_ID = "active_model_id";
    private static final String KEY_ACTIVE_MODEL_NAME = "active_model_name";
    private static final String KEY_PINNED_MODELS = "pinned_models_json";
    private static final String KEY_VOICE_AUTO_SPEAK = "voice_auto_speak";

    private final SharedPreferences prefs;
    private final Gson gson = new Gson();

    public PreferenceManager(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // API Keys
    public String getGeminiApiKey() { return prefs.getString(KEY_GEMINI_API_KEY, ""); }
    public void setGeminiApiKey(String key) { prefs.edit().putString(KEY_GEMINI_API_KEY, key.trim()).apply(); }

    public String getOpenRouterApiKey() { return prefs.getString(KEY_OPENROUTER_API_KEY, ""); }
    public void setOpenRouterApiKey(String key) { prefs.edit().putString(KEY_OPENROUTER_API_KEY, key.trim()).apply(); }

    public String getGroqApiKey() { return prefs.getString(KEY_GROQ_API_KEY, ""); }
    public void setGroqApiKey(String key) { prefs.edit().putString(KEY_GROQ_API_KEY, key.trim()).apply(); }

    public String getNimApiKey() { return prefs.getString(KEY_NIM_API_KEY, ""); }
    public void setNimApiKey(String key) { prefs.edit().putString(KEY_NIM_API_KEY, key.trim()).apply(); }

    public String getOpenAiApiKey() { return prefs.getString(KEY_OPENAI_API_KEY, ""); }
    public void setOpenAiApiKey(String key) { prefs.edit().putString(KEY_OPENAI_API_KEY, key.trim()).apply(); }

    public String getCustomBaseUrl() { return prefs.getString(KEY_CUSTOM_URL, "http://localhost:11434/v1/"); }
    public void setCustomBaseUrl(String url) { prefs.edit().putString(KEY_CUSTOM_URL, url.trim()).apply(); }

    public String getCustomApiKey() { return prefs.getString(KEY_CUSTOM_KEY, ""); }
    public void setCustomApiKey(String key) { prefs.edit().putString(KEY_CUSTOM_KEY, key.trim()).apply(); }

    // Active Selection
    public String getActiveProvider() {
        return prefs.getString(KEY_ACTIVE_PROVIDER, "openrouter");
    }

    public void setActiveProvider(String provider) {
        prefs.edit().putString(KEY_ACTIVE_PROVIDER, provider).apply();
    }

    public String getActiveModelId() {
        return prefs.getString(KEY_ACTIVE_MODEL_ID, "meta-llama/llama-3.3-70b-instruct:free");
    }

    public void setActiveModelId(String modelId) {
        prefs.edit().putString(KEY_ACTIVE_MODEL_ID, modelId).apply();
    }

    public String getActiveModelName() {
        return prefs.getString(KEY_ACTIVE_MODEL_NAME, "Llama 3.3 70B (Free)");
    }

    public void setActiveModelName(String name) {
        prefs.edit().putString(KEY_ACTIVE_MODEL_NAME, name).apply();
    }

    public boolean isVoiceAutoSpeakEnabled() {
        return prefs.getBoolean(KEY_VOICE_AUTO_SPEAK, false);
    }

    public void setVoiceAutoSpeakEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_VOICE_AUTO_SPEAK, enabled).apply();
    }

    // Pinned Models List
    public List<AiModel> getPinnedModels() {
        String json = prefs.getString(KEY_PINNED_MODELS, null);
        if (json == null || json.isEmpty()) {
            List<AiModel> defaultList = getDefaultPresetModels();
            savePinnedModels(defaultList);
            return defaultList;
        }
        Type type = new TypeToken<List<AiModel>>(){}.getType();
        return gson.fromJson(json, type);
    }

    public void savePinnedModels(List<AiModel> models) {
        String json = gson.toJson(models);
        prefs.edit().putString(KEY_PINNED_MODELS, json).apply();
    }

    public static List<AiModel> getDefaultPresetModels() {
        List<AiModel> list = new ArrayList<>();
        // OpenRouter Free Models
        list.add(new AiModel("meta-llama/llama-3.3-70b-instruct:free", "Llama 3.3 70B (Free)", "openrouter", true, true, "Top rated open intelligence"));
        list.add(new AiModel("google/gemini-2.0-flash-exp:free", "Gemini 2.0 Flash (Free)", "openrouter", true, true, "Ultra-fast next-gen multimodal"));
        list.add(new AiModel("deepseek/deepseek-r1:free", "DeepSeek R1 (Free)", "openrouter", true, true, "Advanced reasoning & logic"));
        list.add(new AiModel("qwen/qwen-2.5-coder-32b-instruct:free", "Qwen 2.5 Coder 32B (Free)", "openrouter", true, false, "Coding & problem solving"));
        list.add(new AiModel("mistralai/mistral-7b-instruct:free", "Mistral 7B (Free)", "openrouter", true, false, "Compact high efficiency"));

        // Groq Models
        list.add(new AiModel("llama-3.3-70b-versatile", "Groq Llama 3.3 70B", "groq", true, true, "Lightning speed 500+ T/s"));
        list.add(new AiModel("llama-3.1-8b-instant", "Groq Llama 3.1 8B Instant", "groq", true, false, "Instant sub-second replies"));
        list.add(new AiModel("mixtral-8x7b-32768", "Groq Mixtral 8x7B", "groq", true, false, "MoE architecture with 32k context"));

        // Gemini Native Models
        list.add(new AiModel("gemini-2.0-flash", "Gemini 2.0 Flash", "gemini", true, true, "Direct Google AI endpoint"));
        list.add(new AiModel("gemini-1.5-flash", "Gemini 1.5 Flash", "gemini", true, false, "Fast low-latency Google model"));
        list.add(new AiModel("gemini-1.5-pro", "Gemini 1.5 Pro", "gemini", false, false, "Deep thinking & long context"));

        // OpenAI Models
        list.add(new AiModel("gpt-4o-mini", "GPT-4o Mini", "openai", false, true, "Fast, lightweight ChatGPT model"));
        list.add(new AiModel("gpt-4o", "GPT-4o", "openai", false, false, "Flagship multimodal ChatGPT model"));

        return list;
    }
}
