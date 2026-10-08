package com.subtitle.vtt2srt.domain.translate;

import java.util.List;

/** Chain of Responsibility: tries each translator in order until one succeeds. */
public class FallbackTranslator implements Translator {

    private final List<Translator> chain;

    public FallbackTranslator(List<Translator> chain) {
        this.chain = chain;
    }

    @Override
    public String translate(String text, String sourceLang, String targetLang)
            throws TranslationException {
        TranslationException last = null;
        for (Translator translator : chain) {
            try {
                return translator.translate(text, sourceLang, targetLang);
            } catch (TranslationException e) {
                last = e;
            }
        }
        throw last != null ? last : new TranslationException("No translator configured");
    }
}
