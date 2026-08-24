package com.gnsw.gnsw_backend.controller;

import com.gnsw.gnsw_backend.dto.response.ApiResponse;
import com.gnsw.gnsw_backend.dto.response.ArticleResponse;
import com.gnsw.gnsw_backend.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Public members community feed — no authentication required.
 */
@RestController
@RequestMapping("/api/v1/public/articles")
@RequiredArgsConstructor
public class PublicArticleController {

    private final ArticleService articleService;

    /**
     * Published feed, optionally filtered by feed tag or free-text search.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ArticleResponse>>> listFeed(
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String search) {
        List<ArticleResponse> articles = articleService.listFeed(tag, search);
        return ResponseEntity.ok()
                .body(ApiResponse.<List<ArticleResponse>>builder()
                        .success(true)
                        .message("Feed retrieved.")
                        .data(articles)
                        .build());
    }

    /**
     * Single published article (increments its view count).
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ArticleResponse>> getArticle(@PathVariable UUID id) {
        ArticleResponse article = articleService.getArticle(id);
        return ResponseEntity.ok()
                .body(ApiResponse.<ArticleResponse>builder()
                        .success(true)
                        .message("Article retrieved.")
                        .data(article)
                        .build());
    }

    /**
     * Clap (appreciate) a published article.
     */
    @PostMapping("/{id}/clap")
    public ResponseEntity<ApiResponse<Map<String, Object>>> clap(@PathVariable UUID id) {
        ArticleResponse article = articleService.clapArticle(id);
        return ResponseEntity.ok()
                .body(ApiResponse.<Map<String, Object>>builder()
                        .success(true)
                        .message("Clap registered.")
                        .data(Map.of("claps", article.getClaps()))
                        .build());
    }
}