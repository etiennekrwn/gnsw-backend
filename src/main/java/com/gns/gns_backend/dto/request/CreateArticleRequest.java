package com.gns.gns_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Body for creating a portal article.
 * {@code content} is either a JSON array of rich-text blocks or an HTML string.
 */
@Data
@NoArgsConstructor
public class CreateArticleRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String excerpt;

    @NotBlank(message = "Content is required")
    private String content;

    private List<String> tags;

        private String tag = "For You";

    private String category;

    private String thumbnailUrl;

    private String imageUrl;

    private Integer readTime;

    private String status;
}