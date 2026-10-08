package com.subtitle.vtt2srt.domain.translate;

public enum TranslationEngineType {
    FREE("Free Web Engine (Google & MyMemory)"),
    GEMINI("Google Gemini AI"),
    DEEPSEEK("DeepSeek AI"),
    CLAUDE("Anthropic Claude AI"),
    CUSTOM_OPENAI("Custom OpenAI API / Local LLM");

    private final String displayName;

    TranslationEngineType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static TranslationEngineType fromName(String name) {
        if (name == null) return FREE;
        for (TranslationEngineType type : values()) {
            if (type.name().equalsIgnoreCase(name)) return type;
        }
        return FREE;
    }
}
