package com.subtitle.vtt2srt.domain.parser;

import com.subtitle.vtt2srt.domain.model.SubtitleCue;

import java.util.List;

public interface SubtitleParser {
    List<SubtitleCue> parse(String content) throws SubtitleParseException;
}
