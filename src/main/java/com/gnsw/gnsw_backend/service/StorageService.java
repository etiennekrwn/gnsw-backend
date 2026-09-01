package com.gnsw.gnsw_backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Uploads / deletes binary objects in Supabase Storage.
 *
 * <p>Supabase Storage exposes a simple REST API; we call it directly with a plain
 * JVM HTTP client (same approach as {@code PaymentService} for Paystack) so no heavy
 * SDK dependency is required. Files are persisted in a public bucket and served via
 * a permanent public URL -- not base64, and not the Railway app's ephemeral disk.</p>
 */
@Service
@Slf4j
public class StorageService {

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    /** Base URL, e.g. https://<project-ref>.supabase.co/storage/v1 */
    @Value("${app.storage.supabase-url:}")
    private String supabaseUrl;

    /** Supabase service-role key (server-side only; bypasses RLS). */
    @Value("${app.storage.service-key:}")
    private String serviceKey;

    /** Public bucket name, e.g. "media". */
    @Value("${app.storage.bucket:media}")
    private String bucket;

    /**
     * Upload bytes to the bucket under {@code objectPath} (e.g. "1c34f2e9-....png").
     * Returns the permanent public URL.
     */
    public String upload(String objectPath, byte[] content, String contentType) {
        String url = supabaseUrl + "/object/" + bucket + "/" + objectPath;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + serviceKey)
                .header("Content-Type", contentType != null ? contentType : "application/octet-stream")
                .header("x-upsert", "true")
                .POST(HttpRequest.BodyPublishers.ofByteArray(content))
                .build();

        HttpResponse<String> response = send(request, "upload " + objectPath);
        if (!isSuccess(response.statusCode())) {
            log.error("Storage upload failed ({}): {}", response.statusCode(), response.body());
            throw new IllegalStateException("Failed to upload file to storage.");
        }
        return publicUrl(objectPath);
    }

    /**
     * Delete an object from the bucket. Safe to call even if it does not exist.
     */
    public void delete(String objectPath) {
        if (objectPath == null || objectPath.isBlank()) return;
        String url = supabaseUrl + "/object/" + bucket + "/" + objectPath;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + serviceKey)
                .DELETE()
                .build();

        HttpResponse<String> response = send(request, "delete " + objectPath);
        if (!isSuccess(response.statusCode())) {
            log.warn("Storage delete for {} returned {}: {}", objectPath, response.statusCode(), response.body());
        }
    }

    /** Public URL for an object, e.g. https://.../storage/v1/object/public/media/<path> */
    public String publicUrl(String objectPath) {
        return supabaseUrl + "/object/public/" + bucket + "/" + objectPath;
    }

    private HttpResponse<String> send(HttpRequest request, String action) {
        try {
            return HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Storage request failed (" + action + ").", e);
        }
    }

    private static boolean isSuccess(int status) {
        return status >= 200 && status < 300;
    }
}