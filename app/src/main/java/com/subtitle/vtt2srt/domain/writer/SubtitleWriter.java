package com.subtitle.vtt2srt.domain.writer;

import com.subtitle.vtt2srt.domain.model.ConversionOptions;
import com.subtitle.vtt2srt.domain.model.SubtitleCue;

import java.util.List;

public interface SubtitleWriter {
    String write(List<SubtitleCue> cues, ConversionOptions options);
}
