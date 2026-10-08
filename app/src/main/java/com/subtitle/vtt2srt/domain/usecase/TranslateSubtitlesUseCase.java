package com.subtitle.vtt2srt.domain.usecase;

import com.subtitle.vtt2srt.domain.model.SubtitleCue;
import com.subtitle.vtt2srt.domain.translate.TranslationException;
import com.subtitle.vtt2srt.domain.translate.Translator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** Translates all cues in parallel while preserving order, timing and progress reporting. */
public class TranslateSubtitlesUseCase {

    public interface ProgressListener {
        void onProgress(int done, int total);
    }

    public static final class TranslationReport {
        private final List<SubtitleCue> cues;
        private final int failedCount;

        TranslationReport(List<SubtitleCue> cues, int failedCount) {
            this.cues = cues;
            this.failedCount = failedCount;
        }

        public List<SubtitleCue> getCues() { return cues; }
        public int getFailedCount() { return failedCount; }
    }

    private final Translator translator;
    private final int threads;

    public TranslateSubtitlesUseCase(Translator translator, int threads) {
        this.translator = translator;
        this.threads = Math.max(1, threads);
    }

    public TranslationReport execute(final List<SubtitleCue> cues,
                                     final String sourceLang,
                                     final String targetLang,
                                     final ProgressListener listener,
                                     final AtomicBoolean cancelled) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            final int total = cues.size();
            final AtomicInteger done = new AtomicInteger();
            final AtomicInteger failed = new AtomicInteger();
            List<Future<SubtitleCue>> futures = new ArrayList<>(total);

            for (final SubtitleCue cue : cues) {
                Callable<SubtitleCue> task = new Callable<SubtitleCue>() {
                    @Override
                    public SubtitleCue call() {
                        if (cancelled.get()) throw new CancellationException();
                        SubtitleCue result;
                        if (!containsLetter(cue.getText())) {
                            result = cue.withTranslatedText(cue.getText());   // "...", "♪" etc.
                        } else {
                            try {
                                result = cue.withTranslatedText(
                                        translator.translate(cue.getText(), sourceLang, targetLang));
                            } catch (TranslationException e) {
                                failed.incrementAndGet();
                                result = cue;                                   // keep original text
                            }
                        }
                        if (listener != null) listener.onProgress(done.incrementAndGet(), total);
                        return result;
                    }
                };
                futures.add(pool.submit(task));
            }

            List<SubtitleCue> out = new ArrayList<>(total);
            for (Future<SubtitleCue> future : futures) {
                try {
                    out.add(future.get());
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof CancellationException) throw (CancellationException) cause;
                    throw new IllegalStateException(cause);
                }
            }
            return new TranslationReport(out, failed.get());
        } finally {
            pool.shutdownNow();
        }
    }

    private static boolean containsLetter(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (Character.isLetter(text.charAt(i))) return true;
        }
        return false;
    }
}
