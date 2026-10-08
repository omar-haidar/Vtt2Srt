package com.subtitle.vtt2srt.util;

/** One-shot LiveData payload (toasts, snackbars) that must not replay after rotation. */
public class Event<T> {

    private final T content;
    private boolean handled = false;

    public Event(T content) { this.content = content; }

    public synchronized T getContentIfNotHandled() {
        if (handled) return null;
        handled = true;
        return content;
    }
}
