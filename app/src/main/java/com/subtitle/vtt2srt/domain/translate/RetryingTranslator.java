package com.subtitle.vtt2srt.domain.translate;

/** Decorator: retries a failing translator with a linear back-off. */
public class RetryingTranslator implements Translator {

    private final Translator delegate;
    private final int maxAttempts;
    private final long backoffMs;

    public RetryingTranslator(Translator delegate, int maxAttempts, long backoffMs) {
        this.delegate = delegate;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.backoffMs = backoffMs;
    }

    @Override
    public String translate(String text, String sourceLang, String targetLang)
            throws TranslationException {
        TranslationException last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return delegate.translate(text, sourceLang, targetLang);
            } catch (TranslationException e) {
                last = e;
                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(backoffMs * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new TranslationException("Interrupted", ie);
                    }
                }
            }
        }
        throw last;
    }
}
