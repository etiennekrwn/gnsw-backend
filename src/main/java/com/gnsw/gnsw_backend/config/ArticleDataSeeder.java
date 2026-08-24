package com.gnsw.gnsw_backend.config;

import com.gnsw.gnsw_backend.entity.Article;
import com.gnsw.gnsw_backend.entity.User;
import com.gnsw.gnsw_backend.enums.ArticleStatus;
import com.gnsw.gnsw_backend.repository.ArticleRepository;
import com.gnsw.gnsw_backend.repository.UserRepository;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Seeds the members community feed with sample articles on first run.
 * Mirrors the previous JS-mock feed so the portal has real data to render.
 * Seeding is done in Java because data.sql is not executed for the remote
 * Supabase database by default.
 */
@Component
@RequiredArgsConstructor
public class ArticleDataSeeder implements CommandLineRunner {

    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void run(String... args) {
        if (articleRepository.count() > 0) {
            return;
        }
        User admin = userRepository.findByEmail("admin@gnsw.ng").orElse(null);
        if (admin == null) {
            return;
        }

        add(admin, "How Clear Briefings Help Leaders Make Better Decisions",
                "A strong briefing does more than summarize facts. It gives a leader the context, choices, and language needed to act with confidence.",
                blocks("A strong briefing does more than summarize facts.",
                        "heading:Start with the decision",
                        "Leaders read briefings under time pressure. Lead with the decision, then the facts that support it.",
                        "pullquote:A briefing is a map, not a memo."),
                tags("briefings", "leadership", "clarity"),
                "https://images.unsplash.com/photo-1551836022-d5d88e9218df?auto=format&fit=crop&q=80&w=900",
                "https://images.unsplash.com/photo-1454165804606-c3d57bc86b40?auto=format&fit=crop&q=80&w=1400",
                "Featured", "Leadership", 5, 87, 412, true, publishedAgo(2));

        add(admin, "Writing Speeches That Sound Human on Stage",
                "Memorable speeches are built for the ear first. Rhythm, plain language, and a clear emotional turn make formal remarks feel alive.",
                blocks("Memorable speeches are written for the ear first, not the page.",
                        "heading:Write for the voice",
                        "If a sentence trips the tongue in rehearsal, it will trip the speaker on stage.",
                        "pullquote:The audience hears the sentence before they read it."),
                tags("delivery", "voice", "speechwriting"),
                "https://images.unsplash.com/photo-1475721027785-f74eccf877e2?auto=format&fit=crop&q=80&w=900",
                "https://images.unsplash.com/photo-1511578314322-379afb476865?auto=format&fit=crop&q=80&w=1400",
                "For You", "Culture", 4, 43, 208, false, publishedAgo(4));

        add(admin, "The First Hour After a Public Crisis",
                "The opening response sets the tone for everything that follows.",
                blocks("The opening response sets the tone for everything that follows.",
                        "heading:Establish the facts",
                        "Name what is verified, what is still unknown, and when the next update will come.",
                        "pullquote:Speed matters, but accuracy protects the next statement."),
                tags("crisis", "trust", "public affairs"),
                "https://images.unsplash.com/photo-1497366754035-f200968a6e72?auto=format&fit=crop&q=80&w=900",
                "https://images.unsplash.com/photo-1497366811353-6870744d04b2?auto=format&fit=crop&q=80&w=1400",
                "For You", "Politics", 6, 121, 533, false, publishedAgo(1));

        add(admin, "Turning Policy Detail Into Public Meaning",
                "Citizens rarely need every technical clause. They need to understand what changed, why it matters, and how it affects daily life.",
                blocks("Policy communication fails when it assumes technical accuracy is the same as public meaning.",
                        "heading:Name the human effect",
                        "Every policy message should answer who is affected, what to expect, and where to get help.",
                        "pullquote:People do not reject complexity; they reject being asked to decode it alone."),
                tags("policy", "translation", "citizens"),
                "https://images.unsplash.com/photo-1521791136064-7986c2920216?auto=format&fit=crop&q=80&w=900",
                "https://images.unsplash.com/photo-1556761175-b413da4baf72?auto=format&fit=crop&q=80&w=1400",
                "Latest", "Economy", 5, 64, 310, false, publishedAgo(0));

        add(admin, "What Executive Writers Can Learn From Town Halls",
                "Town halls reveal the questions people actually care about.",
                blocks("Town halls are a live test of institutional language.",
                        "heading:Listen for repetition",
                        "When different people ask the same question in different words, the communication gap is real.",
                        "pullquote:The audience often writes the next draft for you."),
                tags("town halls", "listening"),
                "https://images.unsplash.com/photo-1540575467063-178a50c2df87?auto=format&fit=crop&q=80&w=900",
                "https://images.unsplash.com/photo-1540575467063-178a50c2df87?auto=format&fit=crop&q=80&w=1400",
                "Trending", "Leadership", 7, 152, 640, true, publishedAgo(0));

        addPending(admin, "The Language of Trust in Public Addresses",
                "Trust is built in the small choices: what you name, what you acknowledge, and what you refuse to overstate.",
                blocks("Trust is built in the small choices a communicator makes.",
                        "Name what is uncertain, acknowledge what changed, and never claim more than the evidence supports."),
                tags("trust", "public speaking", "ethics"),
                "https://images.unsplash.com/photo-1505664194779-8beaceb93744?auto=format&fit=crop&q=80&w=900",
                "https://images.unsplash.com/photo-1505664194779-8beaceb93744?auto=format&fit=crop&q=80&w=1400",
                "For You", "Ethics", 3, 0, 0, false);
    }
private void add(User author, String title, String excerpt, String content, String tags,
                     String thumbnailUrl, String imageUrl, String tag, String category,
                     int readTime, int claps, long views, boolean featured, LocalDateTime publishedAt) {
        save(author, title, excerpt, content, tags, thumbnailUrl, imageUrl, tag, category,
                readTime, claps, views, featured, ArticleStatus.PUBLISHED, publishedAt);
    }

    private void addPending(User author, String title, String excerpt, String content, String tags,
                            String thumbnailUrl, String imageUrl, String tag, String category,
                            int readTime, int claps, long views, boolean featured) {
        save(author, title, excerpt, content, tags, thumbnailUrl, imageUrl, tag, category,
                readTime, claps, views, featured, ArticleStatus.PENDING_REVIEW, null);
    }

    private void save(User author, String title, String excerpt, String content, String tags,
                      String thumbnailUrl, String imageUrl, String tag, String category,
                      int readTime, int claps, long views, boolean featured,
                      ArticleStatus status, LocalDateTime publishedAt) {
        articleRepository.save(Article.builder()
                .author(author)
                .title(title)
                .excerpt(excerpt)
                .content(content)
                .tags(tags)
                .thumbnailUrl(thumbnailUrl)
                .imageUrl(imageUrl)
                .tag(tag)
                .category(category)
                .readTime(readTime)
                .claps(claps)
                .views(views)
                .commentCount(6)
                .featured(featured)
                .status(status)
                .publishedAt(publishedAt)
                .build());
    }

    private LocalDateTime publishedAgo(int days) {
        return LocalDateTime.now().minusDays(days);
    }

    private String blocks(String... items) {
        List<Block> list = new ArrayList<>();
        for (String item : items) {
            int colon = item.indexOf(':');
            String type = "paragraph";
            String text = item;
            if (colon > 0) {
                String prefix = item.substring(0, colon);
                if (prefix.equals("heading") || prefix.equals("pullquote")) {
                    type = prefix;
                    text = item.substring(colon + 1);
                }
            }
            list.add(new Block(type, text));
        }
        return toJson(list);
    }

    private String tags(String... tagValues) {
        return toJson(java.util.Arrays.asList(tagValues));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    @Data
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private static class Block {
        private String type;
        private String text;
    }
}