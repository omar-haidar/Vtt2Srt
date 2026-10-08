package com.subtitle.vtt2srt.domain.translate;

import com.subtitle.vtt2srt.data.SimpleHttpClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class GeminiTranslator implements Translator {

    private final SimpleHttpClient http;
    private final AiTranslatorConfig config;

    public GeminiTranslator(SimpleHttpClient http, AiTranslatorConfig config) {
        this.http = http;
        this.config = config;
    }

    @Override
    public String translate(String text, String sourceLang, String targetLang) throws TranslationException {
        if (config.getApiKey().isEmpty()) {
            throw new TranslationException("Gemini API key is missing");
        }
        String model = config.getEffectiveModelName();
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + config.getApiKey();

        String systemPrompt = config.getCustomSystemPrompt().isEmpty()
                ? "You are an expert subtitle translator. Translate the text accurately from " + sourceLang + " to " + targetLang + ". Output ONLY the translated text without explanations, commentary, or surrounding quotes."
                : config.getCustomSystemPrompt();

        try {
            JSONObject partObj = new JSONObject();
            partObj.put("text", systemPrompt + "\n\nSubtitle to translate:\n" + text);

            JSONArray partsArray = new JSONArray();
            partsArray.put(partObj);

            JSONObject contentObj = new JSONObject();
            contentObj.put("parts", partsArray);

            JSONArray contentsArray = new JSONArray();
            contentsArray.put(contentObj);

            JSONObject body = new JSONObject();
            body.put("contents", contentsArray);

            String response = http.postJson(url, body.toString(), null);
            JSONObject root = new JSONObject(response);
            JSONArray candidates = root.getJSONArray("candidates");
            if (candidates.length() == 0) {
                throw new TranslationException("Empty response from Gemini API");
            }
            JSONObject candidate = candidates.getJSONObject(0);
            JSONObject content = candidate.getJSONObject("content");
            JSONArray parts = content.getJSONArray("parts");
            String result = parts.getJSONObject(0).getString("text").trim();
            if (result.startsWith("\"") && result.endsWith("\"") && result.length() > 1) {
                result = result.substring(1, result.length() - 1);
            }
            return result;
        } catch (IOException | JSONException e) {
            throw new TranslationException("Gemini AI translation failed: " + e.getMessage(), e);
        }
    }
}
