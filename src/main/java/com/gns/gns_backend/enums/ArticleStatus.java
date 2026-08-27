package com.gns.gns_backend.enums;

/**
 * Lifecycle of a portal article.
 * Members can publish directly, or submit for admin moderation depending on flow.
 */
public enum ArticleStatus {
    DRAFT,
    PENDING_REVIEW,
    PUBLISHED,
    REJECTED
}