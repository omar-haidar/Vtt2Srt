package com.subtitle.vtt2srt.domain.translate;

import java.util.Objects;

/** Immutable configuration data class for AI translation services. */
public final class AiTranslatorConfig {

    private final TranslationEngineType engineType;
    private final String apiKey;
    private final String modelName;
    private final String baseUrl;
    private final String customSystemPrompt;

    private AiTranslatorConfig(Builder builder) {
        this.engineType = builder.engineType != null ? builder.engineType : TranslationEngineType.FREE;
        this.apiKey = builder.apiKey != null ? builder.apiKey.trim() : "";
        this.modelName = builder.modelName != null ? builder.modelName.trim() : "";
        this.baseUrl = builder.baseUrl != null ? builder.baseUrl.trim() : "";
        this.customSystemPrompt = builder.customSystemPrompt != null ? builder.customSystemPrompt.trim() : "";
    }

    public TranslationEngineType getEngineType() { return engineType; }
    public String getApiKey() { return apiKey; }
    public String getModelName() { return modelName; }
    public String getBaseUrl() { return baseUrl; }
    public String getCustomSystemPrompt() { return customSystemPrompt; }

    public boolean isAiEngine() {
        return engineType != TranslationEngineType.FREE && !apiKey.isEmpty();
    }

    public String getEffectiveModelName() {
        if (!modelName.isEmpty()) return modelName;
        switch (engineType) {
            case GEMINI: return "gemini-1.5-flash";
            case DEEPSEEK: return "deepseek-chat";
            case CLAUDE: return "claude-3-5-haiku-20241022";
            case CUSTOM_OPENAI: return "gpt-4o-mini";
            default: return "";
        }
    }

    public String getEffectiveBaseUrl() {
        if (!baseUrl.isEmpty()) return baseUrl;
        switch (engineType) {
            case DEEPSEEK: return "https://api.deepseek.com/v1";
            case CLAUDE: return "https://api.anthropic.com/v1";
            case CUSTOM_OPENAI: return "https://api.openai.com/v1";
            default: return "";
        }
    }

    public static final class Builder {
        private TranslationEngineType engineType = TranslationEngineType.FREE;
        private String apiKey = "";
        private String modelName = "";
        private String baseUrl = "";
        private String customSystemPrompt = "";

        public Builder engineType(TranslationEngineType engineType) {
            this.engineType = engineType;
            return this;
        }

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder modelName(String modelName) {
            this.modelName = modelName;
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder customSystemPrompt(String customSystemPrompt) {
            this.customSystemPrompt = customSystemPrompt;
            return this;
        }

        public AiTranslatorConfig build() {
            return new AiTranslatorConfig(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AiTranslatorConfig that = (AiTranslatorConfig) o;
        return engineType == that.engineType &&
                Objects.equals(apiKey, that.apiKey) &&
                Objects.equals(modelName, that.modelName) &&
                Objects.equals(baseUrl, that.baseUrl) &&
                Objects.equals(customSystemPrompt, that.customSystemPrompt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(engineType, apiKey, modelName, baseUrl, customSystemPrompt);
    }
}
