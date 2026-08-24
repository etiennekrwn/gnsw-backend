package com.gnsw.gnsw_backend.controller;

import com.gnsw.gnsw_backend.dto.request.CreateArticleRequest;
import com.gnsw.gnsw_backend.dto.response.ApiResponse;
import com.gnsw.gnsw_backend.dto.response.ArticleResponse;
import com.gnsw.gnsw_backend.service.ArticleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Authenticated member endpoints for the community feed — create and list
 * your own published articles.
 */
@RestController
@RequestMapping("/api/v1/members/articles")
@RequiredArgsConstructor
public class MemberArticleController {

    private final ArticleService articleService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ArticleResponse>>> myArticles(Authentication authentication) {
        List<ArticleResponse> articles = articleService.getMyArticles(authentication.getName());
        return ResponseEntity.ok()
                .body(ApiResponse.<List<ArticleResponse>>builder()
                        .success(true)
                        .message("Your articles retrieved.")
                        .data(articles)
                        .build());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ArticleResponse>> create(
            @Valid @RequestBody CreateArticleRequest request,
            Authentication authentication) {
        ArticleResponse article = articleService.createArticle(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<ArticleResponse>builder()
                        .success(true)
                        .message("Article published to the feed.")
                        .data(article)
                        .build());
    }
}