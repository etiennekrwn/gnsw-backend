package com.gns.gns_backend.entity;

import com.gns.gns_backend.enums.ArticleStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A piece of writing published to the members community feed.
 * {@code content} holds either a JSON array of rich-text blocks (feed articles)
 * or an HTML string (drafts saved by the Tiptap editor) — the API exposes both as
 * {@code body} (structured blocks) and {@code bodyHtml}.
 */
@Entity
@Table(name = "articles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String excerpt;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String tags;

    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    private String thumbnailUrl;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(length = 60)
    private String tag;

    @Column(length = 120)
    private String category;

    @Column(name = "read_time")
    private Integer readTime;

    private Integer claps;

    private Long views;

    @Column(name = "comment_count")
    private Integer commentCount;

    private Boolean featured;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ArticleStatus status;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = ArticleStatus.DRAFT;
        if (claps == null) claps = 0;
        if (views == null) views = 0L;
        if (commentCount == null) commentCount = 0;
        if (featured == null) featured = false;
        if (readTime == null) readTime = 0;
        if (tag == null) tag = "For You";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}