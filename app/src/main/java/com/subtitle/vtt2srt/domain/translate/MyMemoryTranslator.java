package com.subtitle.vtt2srt.domain.translate;

import com.subtitle.vtt2srt.data.SimpleHttpClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;

/** Fallback engine: MyMemory public API (free daily quota, needs an explicit source language). */
public class MyMemoryTranslator implements Translator {

    private static final String ENDPOINT = "https://api.mymemory.translated.net/get";

    private final SimpleHttpClient http;

    public MyMemoryTranslator(SimpleHttpClient http) {
        this.http = http;
    }

    @Override
    public String translate(String text, String sourceLang, String targetLang)
            throws TranslationException {
        String source = "auto".equalsIgnoreCase(sourceLang) ? "en" : sourceLang;
        try {
            String url = ENDPOINT
                    + "?q=" + URLEncoder.encode(text, "UTF-8")
                    + "&langpair=" + URLEncoder.encode(source + "|" + targetLang, "UTF-8");
            JSONObject json = new JSONObject(http.get(url));
            String status = json.optString("responseStatus", "200");
            if (!"200".equals(status)) {
                throw new TranslationException("MyMemory status " + status);
            }
            String translated = json.getJSONObject("responseData").getString("translatedText").trim();
            if (translated.isEmpty() || translated.startsWith("MYMEMORY WARNING")) {
                throw new TranslationException("MyMemory quota or empty result");
            }
            return translated;
        } catch (IOException | JSONException e) {
            throw new TranslationException("MyMemory failed: " + e.getMessage(), e);
        }
    }
}
