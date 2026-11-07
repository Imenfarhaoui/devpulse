package com.devpulse.modules.monitoring.service;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

@Component
public class HttpHealthCheckClient {

    private final HttpClient httpClient;

    public HttpHealthCheckClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public CheckExecutionResult executeCheck(String url, String method, int expectedStatusCode, int timeoutMs) {
        long startTime = System.nanoTime();
        try {
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(timeoutMs > 0 ? timeoutMs : 5000))
                    .header("User-Agent", "DevPulse-Monitoring-Bot/1.0");

            if ("HEAD".equalsIgnoreCase(method)) {
                reqBuilder.method("HEAD", HttpRequest.BodyPublishers.noBody());
            } else if ("POST".equalsIgnoreCase(method)) {
                reqBuilder.POST(HttpRequest.BodyPublishers.noBody());
            } else {
                reqBuilder.GET();
            }

            HttpResponse<Void> response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.discarding());
            long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;
            int statusCode = response.statusCode();

            boolean isSuccess = (statusCode == expectedStatusCode) || (expectedStatusCode == 200 && statusCode >= 200 && statusCode < 300);
            String errorMessage = isSuccess ? null : "Expected HTTP " + expectedStatusCode + ", received " + statusCode;

            return new CheckExecutionResult(statusCode, elapsedMs, isSuccess, errorMessage);
        } catch (HttpTimeoutException e) {
            long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;
            return new CheckExecutionResult(null, elapsedMs, false, "Connection timed out after " + timeoutMs + "ms");
        } catch (Exception e) {
            long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return new CheckExecutionResult(null, elapsedMs, false, "Health check error: " + msg);
        }
    }

    public static class CheckExecutionResult {
        private final Integer httpStatus;
        private final long responseTimeMs;
        private final boolean success;
        private final String errorMessage;

        public CheckExecutionResult(Integer httpStatus, long responseTimeMs, boolean success, String errorMessage) {
            this.httpStatus = httpStatus;
            this.responseTimeMs = responseTimeMs;
            this.success = success;
            this.errorMessage = errorMessage;
        }

        public Integer getHttpStatus() { return httpStatus; }
        public long getResponseTimeMs() { return responseTimeMs; }
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
    }
}

