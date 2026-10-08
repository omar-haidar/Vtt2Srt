package com.subtitle.vtt2srt.ui;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.subtitle.vtt2srt.R;
import com.subtitle.vtt2srt.data.SubtitleFileRepository;
import com.subtitle.vtt2srt.domain.model.ConversionOptions;
import com.subtitle.vtt2srt.domain.model.SubtitleCue;
import com.subtitle.vtt2srt.domain.parser.SubtitleParseException;
import com.subtitle.vtt2srt.domain.parser.SubtitleParser;
import com.subtitle.vtt2srt.domain.parser.VttParser;
import com.subtitle.vtt2srt.domain.translate.TranslatorFactory;
import com.subtitle.vtt2srt.domain.usecase.TranslateSubtitlesUseCase;
import com.subtitle.vtt2srt.domain.writer.SrtWriter;
import com.subtitle.vtt2srt.domain.writer.SubtitleWriter;
import com.subtitle.vtt2srt.util.Event;
import com.subtitle.vtt2srt.util.NetworkChecker;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainViewModel extends AndroidViewModel {

    public enum Status { IDLE, LOADED, WORKING, DONE }

    private static final String SOURCE_LANG = "auto";
    private static final String TARGET_LANG = "ar";

    private final SubtitleParser parser = new VttParser();
    private final SubtitleWriter writer = new SrtWriter();
    private final SubtitleFileRepository repository;
    private final TranslateSubtitlesUseCase translateUseCase;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<String> fileName = new MutableLiveData<>();
    private final MutableLiveData<List<SubtitleCue>> cues =
            new MutableLiveData<>(Collections.<SubtitleCue>emptyList());
    private final MutableLiveData<Status> status = new MutableLiveData<>(Status.IDLE);
    private final MutableLiveData<Integer> progress = new MutableLiveData<>(0);
    private final MutableLiveData<Event<String>> message = new MutableLiveData<>();

    private volatile List<SubtitleCue> parsedCues;
    private volatile String srtContent;
    private volatile String baseName = "subtitle";
    private volatile boolean lastRunTranslated;
    private volatile AtomicBoolean cancelToken;
    private volatile Future<?> job;

    public MainViewModel(@NonNull Application application) {
        super(application);
        repository = new SubtitleFileRepository(application.getContentResolver());
        translateUseCase = new TranslateSubtitlesUseCase(TranslatorFactory.createDefault(), 3);
    }

    // ---- observable state ----
    public LiveData<String> getFileName() { return fileName; }
    public LiveData<List<SubtitleCue>> getCues() { return cues; }
    public LiveData<Status> getStatus() { return status; }
    public LiveData<Integer> getProgress() { return progress; }
    public LiveData<Event<String>> getMessage() { return message; }

    // ---- actions ----
    public void loadFile(final Uri uri) {
        if (status.getValue() == Status.WORKING) return;
        ioExecutor.execute(new Runnable() {
            @Override
            public void run() {
                Application app = getApplication();
                try {
                    String name = repository.getDisplayName(uri);
                    List<SubtitleCue> parsed = parser.parse(repository.readText(uri));
                    if (parsed.isEmpty()) {
                        postMessage(app.getString(R.string.err_invalid_vtt));
                        return;
                    }
                    parsedCues = parsed;
                    srtContent = null;
                    baseName = stripExtension(name);
                    fileName.postValue(name);
                    cues.postValue(parsed);
                    progress.postValue(0);
                    status.postValue(Status.LOADED);
                } catch (SubtitleParseException e) {
                    postMessage(app.getString(R.string.err_invalid_vtt));
                } catch (IOException e) {
                    postMessage(app.getString(R.string.err_read));
                }
            }
        });
    }

    public void convert(final ConversionOptions options) {
        final List<SubtitleCue> source = parsedCues;
        if (source == null || status.getValue() == Status.WORKING) return;
        Application app = getApplication();
        if (options.isTranslate() && !NetworkChecker.isOnline(app)) {
            postMessage(app.getString(R.string.err_offline));
            return;
        }
        final AtomicBoolean token = new AtomicBoolean(false);
        cancelToken = token;
        progress.setValue(0);
        status.setValue(Status.WORKING);
        job = ioExecutor.submit(new Runnable() {
            @Override
            public void run() {
                runConversion(source, options, token);
            }
        });
    }

    public void cancel() {
        AtomicBoolean token = cancelToken;
        if (token != null) token.set(true);
        Future<?> current = job;
        if (current != null) current.cancel(true);
    }

    public void saveTo(final Uri uri) {
        final String content = srtContent;
        if (content == null) return;
        ioExecutor.execute(new Runnable() {
            @Override
            public void run() {
                Application app = getApplication();
                try {
                    repository.writeText(uri, content);
                    postMessage(app.getString(R.string.msg_saved));
                } catch (IOException e) {
                    postMessage(app.getString(R.string.err_save));
                }
            }
        });
    }

    public String getSuggestedFileName() {
        return baseName /*+ (lastRunTranslated ? ".ar" : "")*/ + ".srt";
    }

    // ---- internals ----
    private void runConversion(List<SubtitleCue> source, ConversionOptions options,
                               AtomicBoolean token) {
        Application app = getApplication();
        try {
            List<SubtitleCue> result = source;
            int failed = 0;
            if (options.isTranslate()) {
                TranslateSubtitlesUseCase.TranslationReport report = translateUseCase.execute(
                        source, SOURCE_LANG, TARGET_LANG,
                        new TranslateSubtitlesUseCase.ProgressListener() {
                            @Override
                            public void onProgress(int done, int total) {
                                progress.postValue(done * 100 / total);
                            }
                        }, token);
                result = report.getCues();
                failed = report.getFailedCount();
                if (failed >= source.size()) {
                    status.postValue(Status.LOADED);
                    postMessage(app.getString(R.string.err_translate_failed));
                    return;
                }
            }
            srtContent = writer.write(result, options);
            lastRunTranslated = options.isTranslate();
            cues.postValue(result);
            progress.postValue(100);
            status.postValue(Status.DONE);
            postMessage(failed == 0
                    ? app.getString(R.string.msg_done)
                    : app.getString(R.string.msg_done_failed, failed));
        } catch (CancellationException | InterruptedException e) {
            status.postValue(Status.LOADED);
            postMessage(app.getString(R.string.msg_cancelled));
        } catch (Exception e) {
            status.postValue(Status.LOADED);
            postMessage(app.getString(R.string.err_generic));
        }
    }

    private void postMessage(String text) {
        message.postValue(new Event<>(text));
    }

    private static String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    @Override
    protected void onCleared() {
        cancel();
        ioExecutor.shutdownNow();
    }
}
