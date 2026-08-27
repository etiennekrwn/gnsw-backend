package com.gns.gns_backend.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * Author details embedded in an article's feed/detail response.
 */
@Data
@Builder
public class ArticleAuthor {
    private String id;
    private String name;
    private String role;
    private String credential;
    private String bio;
    private String initials;
    private String tier;
}