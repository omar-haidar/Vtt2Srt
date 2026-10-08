package com.subtitle.vtt2srt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.subtitle.vtt2srt.domain.model.ConversionOptions;
import com.subtitle.vtt2srt.domain.model.SubtitleCue;
import com.subtitle.vtt2srt.domain.parser.SubtitleParseException;
import com.subtitle.vtt2srt.domain.parser.VttParser;
import com.subtitle.vtt2srt.domain.translate.AiTranslatorConfig;
import com.subtitle.vtt2srt.domain.translate.TranslationEngineType;
import com.subtitle.vtt2srt.domain.translate.TranslationException;
import com.subtitle.vtt2srt.domain.translate.Translator;
import com.subtitle.vtt2srt.domain.usecase.TranslateSubtitlesUseCase;
import com.subtitle.vtt2srt.domain.writer.SrtWriter;

import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConversionTest {

    private static final String VTT =
            "WEBVTT\r\n\r\n"
            + "NOTE a comment\r\n\r\n"
            + "1\r\n00:00:32.043 --> 00:00:33.043 align:start\r\n<v a>Come on in.\r\n\r\n"
            + "2\r\n01:05.375 --> 01:07.535\r\n<v b>Hello &amp; <i>welcome</i>\r\nsecond line\r\n";

    @Test
    public void parsesCuesTimesSpeakersAndText() throws SubtitleParseException {
        List<SubtitleCue> cues = new VttParser().parse(VTT);
        assertEquals(2, cues.size());
        assertEquals(32043L, cues.get(0).getStartMs());
        assertEquals(33043L, cues.get(0).getEndMs());
        assertEquals("a", cues.get(0).getSpeaker());
        assertEquals("Come on in.", cues.get(0).getText());
        assertEquals(65375L, cues.get(1).getStartMs());
        assertEquals("Hello & welcome\nsecond line", cues.get(1).getText());
    }

    @Test(expected = SubtitleParseException.class)
    public void rejectsNonVtt() throws SubtitleParseException {
        new VttParser().parse("not a subtitle file");
    }

    @Test
    public void writesSrtWithSequentialNumbersAndCommaMillis() throws SubtitleParseException {
        List<SubtitleCue> cues = new VttParser().parse(VTT);
        ConversionOptions options = new ConversionOptions.Builder().translate(false).build();
        String srt = new SrtWriter().write(cues, options);
        assertTrue(srt.startsWith("1\n00:00:32,043 --> 00:00:33,043\nCome on in.\n\n2\n"));
        assertTrue(srt.contains("00:01:05,375 --> 00:01:07,535"));
    }

    @Test
    public void writesArabicWithRtlMarkAndBilingualOriginal() throws SubtitleParseException {
        List<SubtitleCue> cues = new VttParser().parse(VTT);
        SubtitleCue translated = cues.get(0).withTranslatedText("تفضل بالدخول.");
        ConversionOptions options = new ConversionOptions.Builder()
                .translate(true).bilingual(true).rtlMarks(true).build();
        String srt = new SrtWriter().write(Collections.singletonList(translated), options);
        assertTrue(srt.contains("\u200Fتفضل بالدخول.\n<i>Come on in.</i>"));
    }

    @Test
    public void writesNonRtlLanguageWithoutRtlMark() throws SubtitleParseException {
        List<SubtitleCue> cues = new VttParser().parse(VTT);
        SubtitleCue translated = cues.get(0).withTranslatedText("Entre");
        ConversionOptions options = new ConversionOptions.Builder()
                .translate(true).targetLang("es").rtlMarks(false).build();
        String srt = new SrtWriter().write(Collections.singletonList(translated), options);
        assertTrue(srt.contains("Entre"));
        assertFalse(srt.contains("\u200F"));
    }

    @Test
    public void translationKeepsOrderAndCountsFailures() throws Exception {
        List<SubtitleCue> cues = new VttParser().parse(VTT);
        Translator fake = new Translator() {
            @Override
            public String translate(String text, String s, String t) throws TranslationException {
                if (text.startsWith("Come")) throw new TranslationException("boom");
                return "ar:" + text;
            }
        };
        TranslateSubtitlesUseCase.TranslationReport report =
                new TranslateSubtitlesUseCase(fake, 3)
                        .execute(cues, "auto", "ar", null, new AtomicBoolean(false));
        assertEquals(1, report.getFailedCount());
        assertEquals(false, report.getCues().get(0).isTranslated());
        assertEquals("ar:Hello & welcome\nsecond line", report.getCues().get(1).getTranslatedText());
    }

    @Test
    public void aiTranslatorConfigDefaultsAndEffectiveValues() {
        AiTranslatorConfig config = new AiTranslatorConfig.Builder()
                .engineType(TranslationEngineType.GEMINI)
                .apiKey("test-key")
                .build();
        assertTrue(config.isAiEngine());
        assertEquals("gemini-1.5-flash", config.getEffectiveModelName());

        AiTranslatorConfig deepseekConfig = new AiTranslatorConfig.Builder()
                .engineType(TranslationEngineType.DEEPSEEK)
                .apiKey("key")
                .build();
        assertEquals("https://api.deepseek.com/v1", deepseekConfig.getEffectiveBaseUrl());
        assertEquals("deepseek-chat", deepseekConfig.getEffectiveModelName());
    }
}
