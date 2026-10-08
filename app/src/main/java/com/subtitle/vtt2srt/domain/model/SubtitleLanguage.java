package com.subtitle.vtt2srt.domain.model;

import java.util.ArrayList;
import java.util.List;

public class SubtitleLanguage {
    private final String code;
    private final String displayName;
    private final boolean rtl;

    public SubtitleLanguage(String code, String displayName, boolean rtl) {
        this.code = code;
        this.displayName = displayName;
        this.rtl = rtl;
    }

    public String getCode() { return code; }
    public String getDisplayName() { return displayName; }
    public boolean isRtl() { return rtl; }

    @Override
    public String toString() {
        return displayName;
    }

    public static List<SubtitleLanguage> getSupportedLanguages() {
        List<SubtitleLanguage> list = new ArrayList<>();
        list.add(new SubtitleLanguage("ar", "العربية (Arabic)", true));
        list.add(new SubtitleLanguage("en", "English", false));
        list.add(new SubtitleLanguage("es", "Español (Spanish)", false));
        list.add(new SubtitleLanguage("fr", "Français (French)", false));
        list.add(new SubtitleLanguage("de", "Deutsch (German)", false));
        list.add(new SubtitleLanguage("tr", "Türkçe (Turkish)", false));
        list.add(new SubtitleLanguage("fa", "فارسی (Persian)", true));
        list.add(new SubtitleLanguage("ur", "اردو (Urdu)", true));
        list.add(new SubtitleLanguage("he", "עברית (Hebrew)", true));
        list.add(new SubtitleLanguage("ru", "Русский (Russian)", false));
        list.add(new SubtitleLanguage("zh", "中文 (Chinese)", false));
        list.add(new SubtitleLanguage("ja", "日本語 (Japanese)", false));
        list.add(new SubtitleLanguage("ko", "한국어 (Korean)", false));
        list.add(new SubtitleLanguage("it", "Italiano (Italian)", false));
        list.add(new SubtitleLanguage("pt", "Português (Portuguese)", false));
        list.add(new SubtitleLanguage("nl", "Nederlands (Dutch)", false));
        list.add(new SubtitleLanguage("pl", "Polski (Polish)", false));
        list.add(new SubtitleLanguage("id", "Bahasa Indonesia", false));
        list.add(new SubtitleLanguage("vi", "Tiếng Việt (Vietnamese)", false));
        list.add(new SubtitleLanguage("hi", "हिन्दी (Hindi)", false));
        list.add(new SubtitleLanguage("bn", "বাংলা (Bengali)", false));
        list.add(new SubtitleLanguage("th", "ไทย (Thai)", false));
        list.add(new SubtitleLanguage("el", "Ελληνικά (Greek)", false));
        list.add(new SubtitleLanguage("sv", "Svenska (Swedish)", false));
        list.add(new SubtitleLanguage("uk", "Українська (Ukrainian)", false));
        return list;
    }
}
