package com.subtitle.vtt2srt.domain.model;

/** Immutable conversion settings, created through {@link Builder}. */
public final class ConversionOptions {

    private final boolean translate;
    private final boolean bilingual;
    private final boolean keepSpeakers;
    private final boolean rtlMarks;

    private ConversionOptions(Builder b) {
        this.translate = b.translate;
        this.bilingual = b.bilingual;
        this.keepSpeakers = b.keepSpeakers;
        this.rtlMarks = b.rtlMarks;
    }

    public boolean isTranslate() { return translate; }
    public boolean isBilingual() { return bilingual; }
    public boolean isKeepSpeakers() { return keepSpeakers; }
    public boolean isRtlMarks() { return rtlMarks; }

    public static final class Builder {
        private boolean translate = true;
        private boolean bilingual = false;
        private boolean keepSpeakers = false;
        private boolean rtlMarks = true;

        public Builder translate(boolean v) { this.translate = v; return this; }
        public Builder bilingual(boolean v) { this.bilingual = v; return this; }
        public Builder keepSpeakers(boolean v) { this.keepSpeakers = v; return this; }
        public Builder rtlMarks(boolean v) { this.rtlMarks = v; return this; }
        public ConversionOptions build() { return new ConversionOptions(this); }
    }
}
