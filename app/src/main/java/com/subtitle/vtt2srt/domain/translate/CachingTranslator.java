package com.subtitle.vtt2srt.domain.translate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Decorator: repeated lines ("Yeah.", "Mm.") are translated once and reused. */
public class CachingTranslator implements Translator {

    private final Translator delegate;
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public CachingTranslator(Translator delegate) {
        this.delegate = delegate;
    }

    @Override
    public String translate(String text, String sourceLang, String targetLang)
            throws TranslationException {
        String key = sourceLang + "|" + targetLang + "|" + text;
        String cached = cache.get(key);
        if (cached != null) return cached;
        String result = delegate.translate(text, sourceLang, targetLang);
        cache.put(key, result);
        return result;
    }
}
