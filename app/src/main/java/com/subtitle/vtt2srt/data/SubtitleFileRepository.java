package com.subtitle.vtt2srt.data;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/** Reads/writes text files through the Storage Access Framework (no storage permission needed). */
public class SubtitleFileRepository {

    private final ContentResolver resolver;

    public SubtitleFileRepository(ContentResolver resolver) {
        this.resolver = resolver;
    }

    public String readText(Uri uri) throws IOException {
        try (InputStream in = resolver.openInputStream(uri);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (in == null) throw new IOException("Cannot open " + uri);
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    public void writeText(Uri uri, String content) throws IOException {
        try (OutputStream out = resolver.openOutputStream(uri, "wt")) {
            if (out == null) throw new IOException("Cannot open " + uri);
            out.write(content.getBytes(StandardCharsets.UTF_8));
            out.flush();
        }
    }

    public String getDisplayName(Uri uri) {
        try (Cursor cursor = resolver.query(uri, new String[]{OpenableColumns.DISPLAY_NAME},
                null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int col = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (col >= 0) {
                    String name = cursor.getString(col);
                    if (name != null && !name.isEmpty()) return name;
                }
            }
        } catch (RuntimeException ignored) {
            // fall through to the path-based fallback
        }
        String last = uri.getLastPathSegment();
        return last != null ? last : "subtitle.vtt";
    }
}
