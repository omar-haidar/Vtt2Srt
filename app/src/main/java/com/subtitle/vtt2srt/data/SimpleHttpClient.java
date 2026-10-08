package com.subtitle.vtt2srt.data;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/** Minimal HTTP client supporting GET and JSON POST requests. */
public class SimpleHttpClient {

    private static final int CONNECT_TIMEOUT_MS = 15_000;
    private static final int READ_TIMEOUT_MS = 25_000;
    private static final String USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) Vtt2Srt/1.0";

    public String get(String urlString) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
        try {
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setRequestProperty("User-Agent", USER_AGENT);
            return readResponse(conn);
        } finally {
            conn.disconnect();
        }
    }

    public String postJson(String urlString, String jsonBody, Map<String, String> headers) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
        try {
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setDoOutput(true);
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    conn.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }

            byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
            conn.setRequestProperty("Content-Length", String.valueOf(input.length));
            try (OutputStream os = conn.getOutputStream()) {
                os.write(input, 0, input.length);
            }

            return readResponse(conn);
        } finally {
            conn.disconnect();
        }
    }

    private String readResponse(HttpURLConnection conn) throws IOException {
        int code = conn.getResponseCode();
        InputStream stream = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        if (stream == null) {
            throw new IOException("HTTP " + code + " (No response body)");
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int n;
            while ((n = stream.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }
            String body = out.toString("UTF-8");
            if (code < 200 || code >= 300) {
                throw new IOException("HTTP " + code + ": " + body);
            }
            return body;
        }
    }
}
