package com.subtitle.vtt2srt.domain.translate;

import com.subtitle.vtt2srt.data.SimpleHttpClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class OpenAiCompatibleTranslator implements Translator {

    private final SimpleHttpClient http;
    private final AiTranslatorConfig config;

    public OpenAiCompatibleTranslator(SimpleHttpClient http, AiTranslatorConfig config) {
        this.http = http;
        this.config = config;
    }

    @Override
    public String translate(String text, String sourceLang, String targetLang) throws TranslationException {
        if (config.getApiKey().isEmpty()) {
            throw new TranslationException("API key is missing");
        }

        String baseUrl = config.getEffectiveBaseUrl();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        String url = baseUrl + "/chat/completions";

        String systemPrompt = config.getCustomSystemPrompt().isEmpty()
                ? "You are an expert subtitle translator. Translate the given subtitle line from " + sourceLang + " to " + targetLang + ". Return ONLY the translation, with no commentary, no markdown, and no surrounding quotes."
                : config.getCustomSystemPrompt();

        try {
            JSONObject systemMessage = new JSONObject();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);

            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", text);

            JSONArray messages = new JSONArray();
            messages.put(systemMessage);
            messages.put(userMessage);

            JSONObject body = new JSONObject();
            body.put("model", config.getEffectiveModelName());
            body.put("messages", messages);
            body.put("temperature", 0.2);

            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer " + config.getApiKey());

            String response = http.postJson(url, body.toString(), headers);
            JSONObject root = new JSONObject(response);
            JSONArray choices = root.getJSONArray("choices");
            if (choices.length() == 0) {
                throw new TranslationException("Empty response choices");
            }
            JSONObject choice = choices.getJSONObject(0);
            JSONObject message = choice.getJSONObject("message");
            String result = message.getString("content").trim();
            if (result.startsWith("\"") && result.endsWith("\"") && result.length() > 1) {
                result = result.substring(1, result.length() - 1);
            }
            return result;
        } catch (IOException | JSONException e) {
            throw new TranslationException("AI translation failed: " + e.getMessage(), e);
        }
    }
}
