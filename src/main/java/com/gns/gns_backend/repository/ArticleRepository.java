package com.gns.gns_backend.repository;

import com.gns.gns_backend.entity.Article;
import com.gns.gns_backend.enums.ArticleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ArticleRepository extends JpaRepository<Article, UUID> {

    List<Article> findByStatusOrderByPublishedAtDesc(ArticleStatus status);

    List<Article> findByStatusAndTagOrderByPublishedAtDesc(ArticleStatus status, String tag);

    List<Article> findByAuthorIdAndStatusOrderByCreatedAtDesc(UUID authorId, ArticleStatus status);

    Optional<Article> findByIdAndStatus(UUID id, ArticleStatus status);

    List<Article> findByStatusOrderByCreatedAtDesc(ArticleStatus status);

    /**
     * Text search across title / excerpt / category / author name, optionally
     * narrowed to a specific feed tag.
     */
    @Query("SELECT a FROM Article a WHERE a.status = :status " +
            "AND (:tag IS NULL OR :tag = '' OR a.tag = :tag) " +
            "AND (:q IS NULL OR :q = '' " +
            "     OR LOWER(a.title) LIKE %:q% " +
            "     OR LOWER(a.excerpt) LIKE %:q% " +
            "     OR LOWER(a.category) LIKE %:q% " +
            "     OR LOWER(a.author.firstName) LIKE %:q% " +
            "     OR LOWER(a.author.lastName) LIKE %:q%) " +
            "ORDER BY a.publishedAt DESC")
    List<Article> searchFeed(@Param("status") ArticleStatus status,
                             @Param("tag") String tag,
                             @Param("q") String q);
}