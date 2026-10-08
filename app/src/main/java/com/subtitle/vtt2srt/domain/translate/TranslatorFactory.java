package com.subtitle.vtt2srt.domain.translate;

import com.subtitle.vtt2srt.data.SimpleHttpClient;

import java.util.Arrays;

/**
 * Factory class adhering to OCP and Factory Pattern.
 * Assembles the full pipeline: Caching( Fallback / Retry( Engine ) ).
 */
public final class TranslatorFactory {

    private TranslatorFactory() { }

    public static Translator createDefault() {
        return create(new SimpleHttpClient(), new AiTranslatorConfig.Builder().build());
    }

    public static Translator create(SimpleHttpClient http, AiTranslatorConfig config) {
        Translator baseEngine;

        if (config == null || !config.isAiEngine()) {
            Translator google = new RetryingTranslator(new GoogleTranslator(http), 3, 500);
            Translator myMemory = new RetryingTranslator(new MyMemoryTranslator(http), 2, 800);
            baseEngine = new FallbackTranslator(Arrays.asList(google, myMemory));
        } else {
            Translator aiImpl;
            switch (config.getEngineType()) {
                case GEMINI:
                    aiImpl = new GeminiTranslator(http, config);
                    break;
                case CLAUDE:
                    aiImpl = new ClaudeTranslator(http, config);
                    break;
                case DEEPSEEK:
                case CUSTOM_OPENAI:
                default:
                    aiImpl = new OpenAiCompatibleTranslator(http, config);
                    break;
            }
            Translator retryingAi = new RetryingTranslator(aiImpl, 2, 1000);
            Translator fallbackFree = new FallbackTranslator(Arrays.asList(
                    new RetryingTranslator(new GoogleTranslator(http), 2, 500),
                    new RetryingTranslator(new MyMemoryTranslator(http), 1, 800)
            ));
            baseEngine = new FallbackTranslator(Arrays.asList(retryingAi, fallbackFree));
        }

        return new CachingTranslator(baseEngine);
    }
}
