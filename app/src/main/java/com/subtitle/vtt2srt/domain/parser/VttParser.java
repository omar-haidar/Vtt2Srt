package com.subtitle.vtt2srt.domain.parser;

import com.subtitle.vtt2srt.domain.model.SubtitleCue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * WebVTT parser. Handles: BOM, CRLF, optional cue ids, timestamps with or without hours,
 * cue settings after the timing line, NOTE/STYLE/REGION blocks, {@code <v speaker>} voice tags,
 * inline tags ({@code <i>}, {@code <c.x>}, {@code <00:00:01.000>}) and HTML entities.
 */
public class VttParser implements SubtitleParser {

    private static final Pattern TIMING = Pattern.compile(
            "((?:\\d+:)?\\d{2}:\\d{2}[.,]\\d{3})\\s*-->\\s*((?:\\d+:)?\\d{2}:\\d{2}[.,]\\d{3})");
    private static final Pattern VOICE_TAG = Pattern.compile("<v(?:\\.[^\\s>]*)*\\s+([^>]*)>");
    private static final Pattern ANY_TAG = Pattern.compile("<[^>]+>");

    @Override
    public List<SubtitleCue> parse(String content) throws SubtitleParseException {
        if (content == null) {
            throw new SubtitleParseException("Empty content");
        }
        String normalized = content
                .replace("\uFEFF", "")
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .trim();
        if (!normalized.startsWith("WEBVTT")) {
            throw new SubtitleParseException("Missing WEBVTT header");
        }

        List<SubtitleCue> cues = new ArrayList<>();
        String[] blocks = normalized.split("\\n\\s*\\n");
        for (String block : blocks) {
            SubtitleCue cue = parseBlock(block, cues.size() + 1);
            if (cue != null) {
                cues.add(cue);
            }
        }
        return cues;
    }

    private SubtitleCue parseBlock(String block, int nextIndex) {
        String[] lines = block.split("\n");
        int timingLine = -1;
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains("-->")) {
                timingLine = i;
                break;
            }
        }
        if (timingLine < 0) return null;                // header, NOTE, STYLE, REGION

        Matcher timing = TIMING.matcher(lines[timingLine]);
        if (!timing.find()) return null;
        long start = parseTimestamp(timing.group(1));
        long end = parseTimestamp(timing.group(2));

        StringBuilder raw = new StringBuilder();
        for (int i = timingLine + 1; i < lines.length; i++) {
            if (raw.length() > 0) raw.append('\n');
            raw.append(lines[i]);
        }
        String rawText = raw.toString();

        String speaker = null;
        Matcher voice = VOICE_TAG.matcher(rawText);
        if (voice.find()) {
            String name = voice.group(1).trim();
            if (!name.isEmpty()) speaker = name;
        }

        String text = cleanText(rawText);
        if (text.isEmpty()) return null;
        return new SubtitleCue(nextIndex, start, end, speaker, text);
    }

    private String cleanText(String raw) {
        String noTags = ANY_TAG.matcher(raw).replaceAll("");
        String decoded = noTags
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&nbsp;", " ")
                .replace("&lrm;", "")
                .replace("&rlm;", "")
                .replace("&amp;", "&");
        StringBuilder out = new StringBuilder();
        for (String line : decoded.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            if (out.length() > 0) out.append('\n');
            out.append(trimmed);
        }
        return out.toString();
    }

    /** "01:02:03.456" or "02:03.456" (or comma separator) -> milliseconds. */
    private long parseTimestamp(String ts) {
        String[] p = ts.replace(',', '.').split("[:.]");
        long hours = 0;
        long minutes;
        long seconds;
        long millis;
        if (p.length == 4) {
            hours = Long.parseLong(p[0]);
            minutes = Long.parseLong(p[1]);
            seconds = Long.parseLong(p[2]);
            millis = Long.parseLong(p[3]);
        } else {
            minutes = Long.parseLong(p[0]);
            seconds = Long.parseLong(p[1]);
            millis = Long.parseLong(p[2]);
        }
        return ((hours * 60 + minutes) * 60 + seconds) * 1000 + millis;
    }
}
