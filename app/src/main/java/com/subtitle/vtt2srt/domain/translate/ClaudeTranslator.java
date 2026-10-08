package com.subtitle.vtt2srt.domain.translate;

import com.subtitle.vtt2srt.data.SimpleHttpClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ClaudeTranslator implements Translator {

    private final SimpleHttpClient http;
    private final AiTranslatorConfig config;

    public ClaudeTranslator(SimpleHttpClient http, AiTranslatorConfig config) {
        this.http = http;
        this.config = config;
    }

    @Override
    public String translate(String text, String sourceLang, String targetLang) throws TranslationException {
        if (config.getApiKey().isEmpty()) {
            throw new TranslationException("Claude API key is missing");
        }

        String baseUrl = config.getEffectiveBaseUrl();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        String url = baseUrl + "/messages";

        String systemPrompt = config.getCustomSystemPrompt().isEmpty()
                ? "You are an expert subtitle translator. Translate subtitle text from " + sourceLang + " to " + targetLang + " accurately. Return ONLY the translated text without commentary."
                : config.getCustomSystemPrompt();

        try {
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", text);

            JSONArray messages = new JSONArray();
            messages.put(userMessage);

            JSONObject body = new JSONObject();
            body.put("model", config.getEffectiveModelName());
            body.put("max_tokens", 1024);
            body.put("system", systemPrompt);
            body.put("messages", messages);

            Map<String, String> headers = new HashMap<>();
            headers.put("x-api-key", config.getApiKey());
            headers.put("anthropic-version", "2023-06-01");

            String response = http.postJson(url, body.toString(), headers);
            JSONObject root = new JSONObject(response);
            JSONArray content = root.getJSONArray("content");
            if (content.length() == 0) {
                throw new TranslationException("Empty content from Claude API");
            }
            JSONObject first = content.getJSONObject(0);
            String result = first.getString("text").trim();
            if (result.startsWith("\"") && result.endsWith("\"") && result.length() > 1) {
                result = result.substring(1, result.length() - 1);
            }
            return result;
        } catch (IOException | JSONException e) {
            throw new TranslationException("Claude translation failed: " + e.getMessage(), e);
        }
    }
}
