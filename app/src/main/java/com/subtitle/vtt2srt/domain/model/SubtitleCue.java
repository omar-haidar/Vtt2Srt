package com.subtitle.vtt2srt.domain.model;

/** Immutable subtitle cue. Translation produces a new instance via {@link #withTranslatedText}. */
public final class SubtitleCue {

    private final int index;
    private final long startMs;
    private final long endMs;
    private final String speaker;          // nullable
    private final String text;
    private final String translatedText;   // nullable

    public SubtitleCue(int index, long startMs, long endMs, String speaker, String text) {
        this(index, startMs, endMs, speaker, text, null);
    }

    private SubtitleCue(int index, long startMs, long endMs, String speaker,
                        String text, String translatedText) {
        this.index = index;
        this.startMs = startMs;
        this.endMs = endMs;
        this.speaker = speaker;
        this.text = text;
        this.translatedText = translatedText;
    }

    public int getIndex() { return index; }
    public long getStartMs() { return startMs; }
    public long getEndMs() { return endMs; }
    public String getSpeaker() { return speaker; }
    public String getText() { return text; }
    public String getTranslatedText() { return translatedText; }

    public boolean isTranslated() {
        return translatedText != null && !translatedText.isEmpty();
    }

    public SubtitleCue withTranslatedText(String translated) {
        return new SubtitleCue(index, startMs, endMs, speaker, text, translated);
    }
}
