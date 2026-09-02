package com.gnsw.gnsw_backend.controller;

import com.gnsw.gnsw_backend.dto.request.CreateArticleRequest;
import com.gnsw.gnsw_backend.dto.response.ApiResponse;
import com.gnsw.gnsw_backend.dto.response.ArticleResponse;
import com.gnsw.gnsw_backend.entity.AdminUser;
import com.gnsw.gnsw_backend.enums.ArticleStatus;
import com.gnsw.gnsw_backend.service.ArticleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Admin moderation of portal articles — list and approve/reject submissions.
 * All endpoints require the ADMIN role (enforced in SecurityConfig).
 */
@RestController
@RequestMapping("/api/v1/admin/articles")
@RequiredArgsConstructor
public class AdminArticleController {

    private final ArticleService articleService;

    /**
     * List articles, optionally filtered by status (default: PENDING_REVIEW).
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ArticleResponse>>> list(
            @RequestParam(required = false) String status) {
        List<ArticleResponse> articles = articleService.listForAdmin(status);
        return ResponseEntity.ok()
                .body(ApiResponse.<List<ArticleResponse>>builder()
                        .success(true)
                        .message("Articles retrieved.")
                        .data(articles)
                        .build());
    }

    /**
     * Approve (PUBLISHED) or reject an article.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ArticleResponse>> updateStatus(
            @PathVariable UUID id,
            @RequestBody UpdateStatusRequest request) {
        ArticleStatus status = ArticleStatus.valueOf(request.getStatus().toUpperCase());
        ArticleResponse article = articleService.setStatus(id, status);
        return ResponseEntity.ok()
                .body(ApiResponse.<ArticleResponse>builder()
                        .success(true)
                        .message("Article status updated to " + status + ".")
                        .data(article)
                        .build());
    }

        @Data
    public static class UpdateStatusRequest {
        @Pattern(regexp = "PUBLISHED|REJECTED|DRAFT|PENDING_REVIEW")
        private String status;
    }

    /**
     * Fetch a single article by ID (for admin editing/viewing).
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ArticleResponse>> getArticle(@PathVariable UUID id) {
        ArticleResponse article = articleService.getArticleById(id);
        return ResponseEntity.ok()
                .body(ApiResponse.<ArticleResponse>builder()
                        .success(true)
                        .message("Article retrieved.")
                        .data(article)
                        .build());
    }

    /**
     * Create a new article (admin).
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ArticleResponse>> create(
            @Valid @RequestBody CreateArticleRequest request,
            Authentication authentication) {
        Object principal = authentication.getPrincipal();
        String authorIdentity = (principal instanceof AdminUser adminUser)
                ? adminUser.getEmail()
                : authentication.getName();
        ArticleResponse article = articleService.createArticle(authorIdentity, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<ArticleResponse>builder()
                        .success(true)
                        .message("Article created.")
                        .data(article)
                        .build());
    }

    /**
     * Update an existing article (admin).
     */
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<ArticleResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody CreateArticleRequest request) {
        ArticleResponse article = articleService.updateArticle(id, request);
        return ResponseEntity.ok()
                .body(ApiResponse.<ArticleResponse>builder()
                        .success(true)
                        .message("Article updated.")
                        .data(article)
                        .build());
    }

    /**
     * Delete an article (admin).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        articleService.deleteArticle(id);
        return ResponseEntity.ok()
                .body(ApiResponse.<Void>builder()
                        .success(true)
                        .message("Article deleted.")
                        .build());
    }
}