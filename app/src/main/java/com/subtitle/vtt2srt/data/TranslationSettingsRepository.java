package com.subtitle.vtt2srt.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.subtitle.vtt2srt.domain.translate.AiTranslatorConfig;
import com.subtitle.vtt2srt.domain.translate.TranslationEngineType;

public class TranslationSettingsRepository {

    private static final String PREF_NAME = "vtt2srt_translation_settings";
    private static final String KEY_ENGINE_TYPE = "engine_type";
    private static final String KEY_API_KEY = "api_key";
    private static final String KEY_MODEL_NAME = "model_name";
    private static final String KEY_BASE_URL = "base_url";
    private static final String KEY_SYSTEM_PROMPT = "system_prompt";

    private final SharedPreferences prefs;

    public TranslationSettingsRepository(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public AiTranslatorConfig getConfig() {
        String engineStr = prefs.getString(KEY_ENGINE_TYPE, TranslationEngineType.FREE.name());
        TranslationEngineType engineType = TranslationEngineType.fromName(engineStr);
        String apiKey = prefs.getString(KEY_API_KEY, "");
        String modelName = prefs.getString(KEY_MODEL_NAME, "");
        String baseUrl = prefs.getString(KEY_BASE_URL, "");
        String systemPrompt = prefs.getString(KEY_SYSTEM_PROMPT, "");

        return new AiTranslatorConfig.Builder()
                .engineType(engineType)
                .apiKey(apiKey)
                .modelName(modelName)
                .baseUrl(baseUrl)
                .customSystemPrompt(systemPrompt)
                .build();
    }

    public void saveConfig(AiTranslatorConfig config) {
        prefs.edit()
                .putString(KEY_ENGINE_TYPE, config.getEngineType().name())
                .putString(KEY_API_KEY, config.getApiKey())
                .putString(KEY_MODEL_NAME, config.getModelName())
                .putString(KEY_BASE_URL, config.getBaseUrl())
                .putString(KEY_SYSTEM_PROMPT, config.getCustomSystemPrompt())
                .apply();
    }
}
