package com.gnsw.gnsw_backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Article payload returned to the members portal.
 * Field names intentionally mirror the portal's existing article model so the
 * frontend can read them with minimal changes.
 */
@Data
@Builder
public class ArticleResponse {
    private String id;
    private String title;
    private String excerpt;
    private List<String> tags;          // keyword chips rendered on the reader page
    private List<Map<String, Object>> body;    // structured rich-text blocks (paragraph/heading/pullquote)
    private String bodyHtml;            // set when content is HTML (Tiptap drafts)
    private String thumbnail;
    private String image;
    private String thumbnailUrl;        // admin field
    private String imageUrl;            // admin field
    private String tag;                // 'For You' | 'Featured' | 'Latest' | 'Trending'
    private String category;
    private String date;               // human-readable "Jun 25, 2026"
    private String createdAt;          // ISO timestamp for admin
    private String publishedAt;        // ISO timestamp for admin
    private Integer readTime;
    private Integer claps;
    private Integer comments;
    private Long views;
    private Integer likes;             // mirrors claps for the reader engagement bar
    private Boolean featured;
    private String status;             // admin field
    private ArticleAuthor author;
}