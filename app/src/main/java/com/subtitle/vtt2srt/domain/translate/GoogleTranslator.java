package com.subtitle.vtt2srt.domain.translate;

import com.subtitle.vtt2srt.data.SimpleHttpClient;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.net.URLEncoder;

/**
 * Free Google Translate web endpoint (unofficial, no API key).
 * Response shape: [[["translated","original",...], ...], null, "en", ...]
 */
public class GoogleTranslator implements Translator {

    private static final String ENDPOINT = "https://translate.googleapis.com/translate_a/single";

    private final SimpleHttpClient http;

    public GoogleTranslator(SimpleHttpClient http) {
        this.http = http;
    }

    @Override
    public String translate(String text, String sourceLang, String targetLang)
            throws TranslationException {
        try {
            String url = ENDPOINT + "?client=gtx&dt=t"
                    + "&sl=" + URLEncoder.encode(sourceLang, "UTF-8")
                    + "&tl=" + URLEncoder.encode(targetLang, "UTF-8")
                    + "&q=" + URLEncoder.encode(text, "UTF-8");
            JSONArray root = new JSONArray(http.get(url));
            JSONArray segments = root.getJSONArray(0);
            StringBuilder out = new StringBuilder();
            for (int i = 0; i < segments.length(); i++) {
                JSONArray segment = segments.optJSONArray(i);
                if (segment != null && !segment.isNull(0)) {
                    out.append(segment.getString(0));
                }
            }
            String result = out.toString().trim();
            if (result.isEmpty()) {
                throw new TranslationException("Empty translation");
            }
            return result;
        } catch (IOException | JSONException e) {
            throw new TranslationException("Google translate failed: " + e.getMessage(), e);
        }
    }
}
