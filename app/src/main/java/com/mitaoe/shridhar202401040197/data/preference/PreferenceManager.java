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
    private static final String KEY_DEEPSEEK_API_KEY = "deepseek_api_key";
    private static final String KEY_XAI_API_KEY = "xai_api_key";
    private static final String KEY_ANTHROPIC_API_KEY = "anthropic_api_key";
    private static final String KEY_OPENAI_API_KEY = "openai_api_key";
    private static final String KEY_FIREWORKS_API_KEY = "fireworks_api_key";
    private static final String KEY_CUSTOM_URL = "custom_base_url";
    private static final String KEY_CUSTOM_KEY = "custom_api_key";

    private static final String KEY_ACTIVE_PROVIDER = "active_provider";
    private static final String KEY_ACTIVE_MODEL_ID = "active_model_id";
    private static final String KEY_ACTIVE_MODEL_NAME = "active_model_name";
    private static final String KEY_PINNED_MODELS = "pinned_models_json";
    private static final String KEY_FETCHED_MODELS_CACHE = "fetched_models_cache_json";
    private static final String KEY_VOICE_AUTO_SPEAK = "voice_auto_speak";
    private static final String KEY_PREFS_VERSION = "prefs_schema_version";
    private static final int CURRENT_VERSION = 5;

    private final SharedPreferences prefs;
    private final Gson gson = new Gson();

    public PreferenceManager(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        // Wipe legacy hardcoded presets from prior app installs
        int ver = prefs.getInt(KEY_PREFS_VERSION, 0);
        if (ver < CURRENT_VERSION) {
            prefs.edit()
                .remove(KEY_PINNED_MODELS)
                .remove(KEY_FETCHED_MODELS_CACHE)
                .remove(KEY_ACTIVE_MODEL_ID)
                .remove(KEY_ACTIVE_MODEL_NAME)
                .remove(KEY_ACTIVE_PROVIDER)
                .putInt(KEY_PREFS_VERSION, CURRENT_VERSION)
                .apply();
        }
    }

    // Generic Provider API Key Getter & Setter
    public String getApiKey(String provider) {
        if (provider == null) return "";
        switch (provider.toLowerCase()) {
            case "openrouter": return getOpenRouterApiKey();
            case "gemini": return getGeminiApiKey();
            case "groq": return getGroqApiKey();
            case "deepseek": return getDeepSeekApiKey();
            case "xai": return getXaiApiKey();
            case "anthropic": return getAnthropicApiKey();
            case "openai": return getOpenAiApiKey();
            case "fireworks": return getFireworksApiKey();
            case "custom": return getCustomApiKey();
            default: return "";
        }
    }

    public void setApiKey(String provider, String key) {
        if (provider == null) return;
        switch (provider.toLowerCase()) {
            case "openrouter": setOpenRouterApiKey(key); break;
            case "gemini": setGeminiApiKey(key); break;
            case "groq": setGroqApiKey(key); break;
            case "deepseek": setDeepSeekApiKey(key); break;
            case "xai": setXaiApiKey(key); break;
            case "anthropic": setAnthropicApiKey(key); break;
            case "openai": setOpenAiApiKey(key); break;
            case "fireworks": setFireworksApiKey(key); break;
            case "custom": setCustomApiKey(key); break;
        }
    }

    public boolean hasApiKeyFor(String provider) {
        String key = getApiKey(provider);
        return key != null && !key.trim().isEmpty();
    }

    public List<String> getConfiguredProviders() {
        List<String> list = new ArrayList<>();
        String[] all = {"openrouter", "groq", "gemini", "deepseek", "xai", "anthropic", "openai", "fireworks", "custom"};
        for (String p : all) {
            if (hasApiKeyFor(p)) {
                list.add(p);
            }
        }
        return list;
    }

    public static String maskApiKey(String key) {
        if (key == null || key.trim().isEmpty()) return "";
        key = key.trim();
        if (key.length() <= 8) return "••••••••";
        return key.substring(0, 4) + "..." + key.substring(key.length() - 4);
    }

    // Individual API Keys
    public String getGeminiApiKey() { return prefs.getString(KEY_GEMINI_API_KEY, ""); }
    public void setGeminiApiKey(String key) { prefs.edit().putString(KEY_GEMINI_API_KEY, key.trim()).apply(); }

    public String getOpenRouterApiKey() { return prefs.getString(KEY_OPENROUTER_API_KEY, ""); }
    public void setOpenRouterApiKey(String key) { prefs.edit().putString(KEY_OPENROUTER_API_KEY, key.trim()).apply(); }

    public String getGroqApiKey() { return prefs.getString(KEY_GROQ_API_KEY, ""); }
    public void setGroqApiKey(String key) { prefs.edit().putString(KEY_GROQ_API_KEY, key.trim()).apply(); }

    public String getDeepSeekApiKey() { return prefs.getString(KEY_DEEPSEEK_API_KEY, ""); }
    public void setDeepSeekApiKey(String key) { prefs.edit().putString(KEY_DEEPSEEK_API_KEY, key.trim()).apply(); }

    public String getXaiApiKey() { return prefs.getString(KEY_XAI_API_KEY, ""); }
    public void setXaiApiKey(String key) { prefs.edit().putString(KEY_XAI_API_KEY, key.trim()).apply(); }

    public String getAnthropicApiKey() { return prefs.getString(KEY_ANTHROPIC_API_KEY, ""); }
    public void setAnthropicApiKey(String key) { prefs.edit().putString(KEY_ANTHROPIC_API_KEY, key.trim()).apply(); }

    public String getOpenAiApiKey() { return prefs.getString(KEY_OPENAI_API_KEY, ""); }
    public void setOpenAiApiKey(String key) { prefs.edit().putString(KEY_OPENAI_API_KEY, key.trim()).apply(); }

    public String getFireworksApiKey() { return prefs.getString(KEY_FIREWORKS_API_KEY, ""); }
    public void setFireworksApiKey(String key) { prefs.edit().putString(KEY_FIREWORKS_API_KEY, key.trim()).apply(); }

    public String getCustomBaseUrl() { return prefs.getString(KEY_CUSTOM_URL, "http://localhost:11434/v1/"); }
    public void setCustomBaseUrl(String url) { prefs.edit().putString(KEY_CUSTOM_URL, url.trim()).apply(); }

    public String getCustomApiKey() { return prefs.getString(KEY_CUSTOM_KEY, ""); }
    public void setCustomApiKey(String key) { prefs.edit().putString(KEY_CUSTOM_KEY, key.trim()).apply(); }

    // Active Selection (No hardcoded default models)
    public String getActiveProvider() {
        return prefs.getString(KEY_ACTIVE_PROVIDER, "");
    }

    public void setActiveProvider(String provider) {
        prefs.edit().putString(KEY_ACTIVE_PROVIDER, provider).apply();
    }

    public String getActiveModelId() {
        return prefs.getString(KEY_ACTIVE_MODEL_ID, "");
    }

    public void setActiveModelId(String modelId) {
        prefs.edit().putString(KEY_ACTIVE_MODEL_ID, modelId).apply();
    }

    public String getActiveModelName() {
        return prefs.getString(KEY_ACTIVE_MODEL_NAME, "");
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

    // Pinned Models List (Starts clean, NO hardcoded default dummy models)
    public List<AiModel> getPinnedModels() {
        String json = prefs.getString(KEY_PINNED_MODELS, null);
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        Type type = new TypeToken<List<AiModel>>(){}.getType();
        List<AiModel> list = gson.fromJson(json, type);
        return list != null ? list : new ArrayList<>();
    }

    public void savePinnedModels(List<AiModel> models) {
        String json = gson.toJson(models);
        prefs.edit().putString(KEY_PINNED_MODELS, json).apply();
    }

    public boolean isModelPinned(String modelId) {
        if (modelId == null || modelId.isEmpty()) return false;
        List<AiModel> pinned = getPinnedModels();
        for (AiModel m : pinned) {
            if (modelId.equalsIgnoreCase(m.getId())) return true;
        }
        return false;
    }

    public void pinModel(AiModel model) {
        if (model == null) return;
        List<AiModel> pinned = getPinnedModels();
        boolean exists = false;
        for (AiModel m : pinned) {
            if (m.getId().equalsIgnoreCase(model.getId())) {
                exists = true;
                m.setPinned(true);
                break;
            }
        }
        if (!exists) {
            model.setPinned(true);
            pinned.add(model);
        }
        savePinnedModels(pinned);
    }

    public void unpinModel(String modelId) {
        if (modelId == null) return;
        List<AiModel> pinned = getPinnedModels();
        pinned.removeIf(m -> modelId.equalsIgnoreCase(m.getId()));
        savePinnedModels(pinned);
    }

    // Cached Fetched Live Models
    public List<AiModel> getCachedFetchedModels() {
        String json = prefs.getString(KEY_FETCHED_MODELS_CACHE, null);
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        Type type = new TypeToken<List<AiModel>>(){}.getType();
        List<AiModel> list = gson.fromJson(json, type);
        if (list == null) return new ArrayList<>();

        // Sync with pinned status
        List<AiModel> pinned = getPinnedModels();
        for (AiModel m : list) {
            boolean isP = false;
            for (AiModel p : pinned) {
                if (p.getId().equalsIgnoreCase(m.getId())) {
                    isP = true;
                    break;
                }
            }
            m.setPinned(isP);
        }
        return list;
    }

    public void saveCachedFetchedModels(List<AiModel> models) {
        String json = gson.toJson(models);
        prefs.edit().putString(KEY_FETCHED_MODELS_CACHE, json).apply();
    }

    public void mergeCachedFetchedModels(List<AiModel> newModels) {
        if (newModels == null || newModels.isEmpty()) return;
        List<AiModel> cached = getCachedFetchedModels();
        for (AiModel incoming : newModels) {
            boolean exists = false;
            for (int i = 0; i < cached.size(); i++) {
                if (cached.get(i).getId().equalsIgnoreCase(incoming.getId())) {
                    cached.set(i, incoming);
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                cached.add(incoming);
            }
        }
        saveCachedFetchedModels(cached);
    }
}
