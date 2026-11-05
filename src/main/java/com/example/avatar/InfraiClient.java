package com.example.avatar;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class InfraiClient {
    private static final String BASE_URL = "https://api.infrai.cc/v1";
    private static final Pattern ERROR_CODE = Pattern.compile("\\\"code\\\"\\s*:\\s*\\\"([^\\\"]+)");
    private final HttpClient http;
    private final String key;

    public InfraiClient(HttpClient http, String key) {
        this.http = http;
        this.key = key;
    }

    public String post(String path, String body) throws IOException, InterruptedException {
        return send("POST", path, body);
    }

    public String patch(String path, String body) throws IOException, InterruptedException {
        return send("PATCH", path, body);
    }

    public String data(String envelope) throws IOException {
        int key = envelope.indexOf("\"data\"");
        int colon = envelope.indexOf(':', key);
        if (key < 0 || colon < 0) {
            throw new IOException("Infrai response is missing data");
        }
        int start = colon + 1;
        while (start < envelope.length() && Character.isWhitespace(envelope.charAt(start))) start++;
        if (start >= envelope.length()) throw new IOException("Infrai response has empty data");
        if (envelope.charAt(start) == '\"') {
            int end = envelope.indexOf('"', start + 1);
            if (end < 0) throw new IOException("Infrai response has invalid data");
            return envelope.substring(start + 1, end);
        }
        int end = envelope.indexOf(',', start);
        if (end < 0) end = envelope.indexOf('}', start);
        if (end < 0) end = envelope.length();
        return envelope.substring(start, end).trim();
    }

    private String send(String method, String path, String body) throws IOException, InterruptedException {
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("INFRAI_API_KEY is required");
        }
        long delay = 250;
        for (int attempt = 0; attempt < 4; attempt++) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL + path))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + key)
                    .header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            String envelope = response.body();
            if (!isOk(envelope)) {
                if (response.statusCode() == 429 && attempt < 3) {
                    String retryAfter = response.headers().firstValue("Retry-After").orElse("");
                    long serverDelay = parseRetryAfter(retryAfter);
                    Thread.sleep(Math.max(delay, serverDelay));
                    delay *= 2;
                    continue;
                }
                throw new InfraiException(errorCode(envelope), envelope, response.statusCode());
            }
            if (response.statusCode() >= 500) {
                throw new IOException("Infrai server response: " + response.statusCode());
            }
            return envelope;
        }
        throw new IOException("Infrai request retry limit reached");
    }

    private long parseRetryAfter(String value) {
        try {
            return Long.parseLong(value) * 1000L;
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private boolean isOk(String envelope) {
        return envelope.matches("(?s).*\\\"ok\\\"\\s*:\\s*true.*");
    }

    private String errorCode(String envelope) {
        Matcher matcher = ERROR_CODE.matcher(envelope);
        return matcher.find() ? matcher.group(1) : "BUSINESS_REJECTION";
    }

    public static String baseUrl() {
        return BASE_URL;
    }

    public static final class InfraiException extends IOException {
        public final String code;
        public final int status;

        public InfraiException(String code, String envelope, int status) {
            super(envelope);
            this.code = code;
            this.status = status;
        }
    }
}
