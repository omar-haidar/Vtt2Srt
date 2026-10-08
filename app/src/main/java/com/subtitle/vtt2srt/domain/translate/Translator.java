package com.subtitle.vtt2srt.domain.translate;

/** Strategy interface: any online/offline engine can be plugged in. */
public interface Translator {
    String translate(String text, String sourceLang, String targetLang) throws TranslationException;
}
