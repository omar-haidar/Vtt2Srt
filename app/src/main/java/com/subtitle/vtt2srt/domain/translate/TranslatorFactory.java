package com.subtitle.vtt2srt.domain.translate;

import com.subtitle.vtt2srt.data.SimpleHttpClient;

import java.util.Arrays;

/** Factory: Cache( Fallback( Retry(Google), Retry(MyMemory) ) ). */
public final class TranslatorFactory {

    private TranslatorFactory() { }

    public static Translator createDefault() {
        SimpleHttpClient http = new SimpleHttpClient();
        Translator google = new RetryingTranslator(new GoogleTranslator(http), 3, 500);
        Translator myMemory = new RetryingTranslator(new MyMemoryTranslator(http), 2, 800);
        return new CachingTranslator(new FallbackTranslator(Arrays.asList(google, myMemory)));
    }
}
