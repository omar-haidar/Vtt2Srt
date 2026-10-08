package com.subtitle.vtt2srt.domain.writer;

import com.subtitle.vtt2srt.domain.model.ConversionOptions;
import com.subtitle.vtt2srt.domain.model.SubtitleCue;
import com.subtitle.vtt2srt.util.TimeFormatter;

import java.util.List;

public class SrtWriter implements SubtitleWriter {

    /** Right-to-left mark: forces RTL paragraph direction so Arabic punctuation lands correctly. */
    static final char RLM = '\u200F';

    @Override
    public String write(List<SubtitleCue> cues, ConversionOptions options) {
        StringBuilder sb = new StringBuilder();
        int number = 1;
        for (SubtitleCue cue : cues) {
            sb.append(number++).append('\n')
              .append(TimeFormatter.toSrt(cue.getStartMs()))
              .append(" --> ")
              .append(TimeFormatter.toSrt(cue.getEndMs())).append('\n')
              .append(buildBody(cue, options)).append("\n\n");
        }
        return sb.toString();
    }

    private String buildBody(SubtitleCue cue, ConversionOptions options) {
        String prefix = (options.isKeepSpeakers() && cue.getSpeaker() != null)
                ? cue.getSpeaker() + ": " : "";
        String original = prefix + cue.getText();

        if (!options.isTranslate() || !cue.isTranslated()) {
            return original;                       // translation off, or this cue failed to translate
        }
        String arabic = prefix + cue.getTranslatedText();
        if (options.isRtlMarks()) {
            arabic = prefixEachLine(arabic);
        }
        if (options.isBilingual()) {
            return arabic + "\n" + italicEachLine(original);
        }
        return arabic;
    }

    private String prefixEachLine(String text) {
        StringBuilder out = new StringBuilder();
        for (String line : text.split("\n")) {
            if (out.length() > 0) out.append('\n');
            out.append(RLM).append(line);
        }
        return out.toString();
    }

    private String italicEachLine(String text) {
        StringBuilder out = new StringBuilder();
        for (String line : text.split("\n")) {
            if (out.length() > 0) out.append('\n');
            out.append("<i>").append(line).append("</i>");
        }
        return out.toString();
    }
}
