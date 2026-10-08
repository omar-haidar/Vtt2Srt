package com.subtitle.vtt2srt.util;

import java.util.Locale;

public final class TimeFormatter {

    private TimeFormatter() { }

    /** 32043 -> "00:00:32,043" (SRT format). */
    public static String toSrt(long millis) {
        long ms = Math.max(0, millis);
        long hours = ms / 3_600_000L;
        ms %= 3_600_000L;
        long minutes = ms / 60_000L;
        ms %= 60_000L;
        long seconds = ms / 1000L;
        long millisPart = ms % 1000L;
        return String.format(Locale.US, "%02d:%02d:%02d,%03d", hours, minutes, seconds, millisPart);
    }
}
