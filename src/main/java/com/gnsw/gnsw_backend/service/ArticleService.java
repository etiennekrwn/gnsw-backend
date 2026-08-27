package com.gnsw.gnsw_backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gnsw.gnsw_backend.dto.request.CreateArticleRequest;
import com.gnsw.gnsw_backend.dto.response.ArticleAuthor;
import com.gnsw.gnsw_backend.dto.response.ArticleResponse;
import com.gnsw.gnsw_backend.entity.Article;
import com.gnsw.gnsw_backend.entity.Member;
import com.gnsw.gnsw_backend.entity.User;
import com.gnsw.gnsw_backend.enums.ArticleStatus;
import com.gnsw.gnsw_backend.repository.ArticleRepository;
import com.gnsw.gnsw_backend.repository.MemberRepository;
import com.gnsw.gnsw_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Members community feed — real articles persisted in the database.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ArticleService {

    private static final DateTimeFormatter DISPLAY_DATE =
            DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);

    private final ArticleRepository articleRepository;
    private final MemberRepository memberRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    // ------------------------------------------------------------------
    // Public feed
    // ------------------------------------------------------------------

    public List<ArticleResponse> listFeed(String tag, String search) {
        List<Article> articles;
        if (search != null && !search.isBlank()) {
            articles = articleRepository.searchFeed(ArticleStatus.PUBLISHED, normalizedTag(tag), search.trim());
        } else if (tag != null && !tag.isBlank() && !"All".equalsIgnoreCase(tag)) {
            articles = articleRepository.findByStatusAndTagOrderByPublishedAtDesc(ArticleStatus.PUBLISHED, tag);
        } else {
            articles = articleRepository.findByStatusOrderByPublishedAtDesc(ArticleStatus.PUBLISHED);
        }
        return articles.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public ArticleResponse getArticle(UUID id) {
        Article article = articleRepository.findByIdAndStatus(id, ArticleStatus.PUBLISHED)
                .orElseThrow(() -> new IllegalArgumentException("Article not found."));
        incrementViews(article);
        return toResponse(article);
    }

    public ArticleResponse clapArticle(UUID id) {
        Article article = articleRepository.findByIdAndStatus(id, ArticleStatus.PUBLISHED)
                .orElseThrow(() -> new IllegalArgumentException("Article not found."));
        article.setClaps(article.getClaps() == null ? 1 : article.getClaps() + 1);
        articleRepository.save(article);
        return toResponse(article);
    }

    // ------------------------------------------------------------------
    // Authoring (members)
    // ------------------------------------------------------------------

    public List<ArticleResponse> getMyArticles(String username) {
        User author = findByUsername(username);
        return articleRepository.findByAuthorIdAndStatusOrderByCreatedAtDesc(
                        author.getId(), ArticleStatus.PUBLISHED)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public ArticleResponse createArticle(String username, CreateArticleRequest request) {
        User author = findByUsername(username);

        Article article = Article.builder()
                .author(author)
                .title(request.getTitle().trim())
                .excerpt(request.getExcerpt())
                .content(request.getContent())
                .tags(toJsonString(request.getTags()))
                .thumbnailUrl(request.getThumbnailUrl())
                .imageUrl(request.getImageUrl())
                .tag(request.getTag() == null || request.getTag().isBlank() ? "For You" : request.getTag())
                .category(request.getCategory())
                .readTime(request.getReadTime() != null ? request.getReadTime() : guessReadTime(request.getContent()))
                .claps(0)
                .views(0L)
                .commentCount(0)
                .featured(false)
                .status(ArticleStatus.PUBLISHED)
                .publishedAt(LocalDateTime.now())
                .build();

        article = articleRepository.save(article);
        return toResponse(article);
    }

    // ------------------------------------------------------------------
    // Admin moderation
    // ------------------------------------------------------------------

    public List<ArticleResponse> listForAdmin(String status) {
        List<Article> articles;
        if (status != null && !status.isBlank()) {
            try {
                ArticleStatus s = ArticleStatus.valueOf(status.toUpperCase(Locale.ROOT));
                articles = articleRepository.findByStatusOrderByCreatedAtDesc(s);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Unknown article status: " + status);
            }
        } else {
            articles = articleRepository.findByStatusOrderByCreatedAtDesc(ArticleStatus.PENDING_REVIEW);
        }
        return articles.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public ArticleResponse setStatus(UUID id, ArticleStatus status) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Article not found."));
        article.setStatus(status);
        if (status == ArticleStatus.PUBLISHED && article.getPublishedAt() == null) {
            article.setPublishedAt(LocalDateTime.now());
        }
        articleRepository.save(article);
        return toResponse(article);
    }
// ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
    }

    private String normalizedTag(String tag) {
        return (tag == null || tag.isBlank() || "All".equalsIgnoreCase(tag)) ? null : tag;
    }

    private void incrementViews(Article article) {
        article.setViews((article.getViews() == null ? 0L : article.getViews()) + 1L);
        articleRepository.save(article);
    }

    private ArticleResponse toResponse(Article article) {
        List<Map<String, Object>> body = null;
        String bodyHtml = null;
        String content = article.getContent();
        if (content != null) {
            if (looksLikeJsonArray(content)) {
                body = parseBlocks(content);
            } else if (!content.isBlank()) {
                bodyHtml = content;
            }
        }

        List<String> tags = parseTags(article.getTags());
        int claps = article.getClaps() == null ? 0 : article.getClaps();

        return ArticleResponse.builder()
                .id(article.getId().toString())
                .title(article.getTitle())
                .excerpt(article.getExcerpt())
                .tags(tags)
                .body(body)
                .bodyHtml(bodyHtml)
                .thumbnail(article.getThumbnailUrl())
                .image(article.getImageUrl())
                .tag(article.getTag())
                .category(article.getCategory())
                .date(article.getPublishedAt() != null ? DISPLAY_DATE.format(article.getPublishedAt()) : null)
                .readTime(article.getReadTime())
                .claps(claps)
                .comments(article.getCommentCount())
                .views(article.getViews())
                .likes(claps)
                                .featured(article.getFeatured())
                .status(article.getStatus() != null ? article.getStatus().name() : null)
                .createdAt(article.getCreatedAt() != null ? article.getCreatedAt().toString() : null)
                .publishedAt(article.getPublishedAt() != null ? article.getPublishedAt().toString() : null)
                .thumbnailUrl(article.getThumbnailUrl())
                .imageUrl(article.getImageUrl())
                .author(buildAuthor(article.getAuthor()))
                .build();
    }

        public void deleteArticle(UUID id) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Article not found."));
        articleRepository.delete(article);
    }

    public ArticleResponse getArticleById(UUID id) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Article not found."));
        return toResponse(article);
    }

    public ArticleResponse updateArticle(UUID id, CreateArticleRequest request) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Article not found."));
        article.setTitle(request.getTitle());
        article.setExcerpt(request.getExcerpt());
        article.setContent(request.getContent());
        article.setTags(toJsonString(request.getTags()));
        article.setTag(request.getTag() != null ? request.getTag() : "For You");
        article.setCategory(request.getCategory());
        article.setThumbnailUrl(request.getThumbnailUrl());
        article.setImageUrl(request.getImageUrl());
        article.setReadTime(request.getReadTime() != null ? request.getReadTime() : guessReadTime(request.getContent()));
        if (request.getStatus() != null) {
            article.setStatus(ArticleStatus.valueOf(request.getStatus().toUpperCase()));
            if (article.getStatus() == ArticleStatus.PUBLISHED && article.getPublishedAt() == null) {
                article.setPublishedAt(LocalDateTime.now());
            }
        }
        articleRepository.save(article);
        return toResponse(article);
    }

    private ArticleAuthor buildAuthor(User user) {
        Member member = memberRepository.findByUserId(user.getId()).orElse(null);
        String firstName = user.getFirstName() == null ? "" : user.getFirstName();
        String lastName = user.getLastName() == null ? "" : user.getLastName();
        String name = (firstName + " " + lastName).trim();
        String initials = initialsOf(firstName, lastName);
        String tierLabel = user.getTier() != null ? titleCase(user.getTier().name()) : "Member";
        String bio = member != null && member.getBio() != null && !member.getBio().isBlank()
                ? member.getBio()
                : (name.isEmpty() ? "A Guild member." : name + " is a Guild member.");

        return ArticleAuthor.builder()
                .id(user.getId().toString())
                .name(name.isEmpty() ? user.getUsername() : name)
                .role("GNS " + tierLabel)
                .credential(member != null && member.getOrganisation() != null ? member.getOrganisation() : "")
                .bio(bio)
                .initials(initials.isEmpty() ? "GN" : initials)
                .tier(tierLabel)
                .build();
    }

    private String initialsOf(String first, String last) {
        StringBuilder sb = new StringBuilder();
        if (first != null && !first.isEmpty()) sb.append(first.charAt(0));
        if (last != null && !last.isEmpty()) sb.append(last.charAt(0));
        return sb.toString().toUpperCase(Locale.ROOT);
    }

    private String titleCase(String value) {
        if (value == null || value.isEmpty()) return value;
        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1).toLowerCase(Locale.ROOT);
    }

    private boolean looksLikeJsonArray(String value) {
        if (value == null) return false;
        String trimmed = value.trim();
        return trimmed.startsWith("[") && trimmed.endsWith("]");
    }

    private List<Map<String, Object>> parseBlocks(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }

    private List<String> parseTags(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }

    private String toJsonString(List<String> values) {
        if (values == null) return null;
        try {
            return objectMapper.writeValueAsString(values);
        } catch (Exception ex) {
            return null;
        }
    }

    private int guessReadTime(String content) {
        if (content == null) return 0;
        String text = content.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
        int words = text.isEmpty() ? 0 : text.split(" ").length;
        return Math.max(1, (int) Math.ceil(words / 200.0));
    }
}