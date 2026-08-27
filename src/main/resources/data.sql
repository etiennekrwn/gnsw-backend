-- Admin seed user - Run this ONCE after tables are created
-- Password: Admin@12345 (BCrypt hashed)
-- IMPORTANT: Change this password after first login!

INSERT INTO users (id, email, first_name, last_name, address_line1, city, state_province, zip_postal_code, country, tier, status, role, password_hash, username, created_at, updated_at)
VALUES (gen_random_uuid(), 'admin@gnsw.ng', 'Super', 'Admin', 'GNS HQ', 'Lagos', 'Lagos State', '100001', 'Nigeria', 'FELLOW', 'ACCEPTED', 'ROLE_ADMIN',
'$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'admin', NOW(), NOW())
ON CONFLICT (email) DO NOTHING;
-- =========================================================================
-- Portal feed seed articles (the community "social media" content)
-- Author is the seeded admin account; safe to re-run thanks to the
-- NOT EXISTS guards (data.sql executes on every startup).
-- =========================================================================

INSERT INTO articles (id, author_id, title, excerpt, content, tags, thumbnail_url, image_url, tag, category, read_time, claps, views, comment_count, featured, status, published_at, created_at, updated_at)
SELECT gen_random_uuid(), u.id,
'How Clear Briefings Help Leaders Make Better Decisions',
'A strong briefing does more than summarize facts. It gives a leader the context, choices, and language needed to act with confidence.',
'[{"type":"paragraph","text":"A strong briefing does more than summarize facts. It gives a leader the context, choices, and language needed to act with confidence."},{"type":"heading","text":"Start with the decision"},{"type":"paragraph","text":"Leaders read briefings under time pressure. Lead with the decision being asked for, then the facts that support it, then the trade-offs."},{"type":"pullquote","text":"A briefing is a map, not a memo."},{"type":"paragraph","text":"Keep options visible, name what would change their minds, and end with the language they can say out loud."}]',
'["briefings","leadership","clarity"]',
'https://images.unsplash.com/photo-1551836022-d5d88e9218df?auto=format&fit=crop&q=80&w=900',
'https://images.unsplash.com/photo-1454165804606-c3d57bc86b40?auto=format&fit=crop&q=80&w=1400',
'Featured', 'Leadership', 5, 87, 412, 9, TRUE, 'PUBLISHED', NOW() - INTERVAL '2 days', NOW(), NOW()
FROM users u WHERE u.email = 'admin@gnsw.ng'
AND NOT EXISTS (SELECT 1 FROM articles a WHERE a.title = 'How Clear Briefings Help Leaders Make Better Decisions');

INSERT INTO articles (id, author_id, title, excerpt, content, tags, thumbnail_url, image_url, tag, category, read_time, claps, views, comment_count, featured, status, published_at, created_at, updated_at)
SELECT gen_random_uuid(), u.id,
'Writing Speeches That Sound Human on Stage',
'Memorable speeches are built for the ear first. Rhythm, plain language, and a clear emotional turn can make formal remarks feel alive.',
'[{"type":"paragraph","text":"Memorable speeches are built for the ear first, not the page."},{"type":"heading","text":"Write for the voice"},{"type":"paragraph","text":"Read every line aloud. If a sentence trips the tongue in rehearsal, it will trip the speaker on stage."},{"type":"pullquote","text":"The audience hears the sentence before they read it."},{"type":"paragraph","text":"Shorten the sentences, keep the argument visible, and give the speaker one emotional turn to land on."}]',
'["delivery","voice","speechwriting"]',
'https://images.unsplash.com/photo-1475721027785-f74eccf877e2?auto=format&fit=crop&q=80&w=900',
'https://images.unsplash.com/photo-1511578314322-379afb476865?auto=format&fit=crop&q=80&w=1400',
'For You', 'Culture', 4, 43, 208, 4, FALSE, 'PUBLISHED', NOW() - INTERVAL '4 days', NOW(), NOW()
FROM users u WHERE u.email = 'admin@gnsw.ng'
AND NOT EXISTS (SELECT 1 FROM articles a WHERE a.title = 'Writing Speeches That Sound Human on Stage');
INSERT INTO articles (id, author_id, title, excerpt, content, tags, thumbnail_url, image_url, tag, category, read_time, claps, views, comment_count, featured, status, published_at, created_at, updated_at)
SELECT gen_random_uuid(), u.id,
'The First Hour After a Public Crisis',
'The opening response sets the tone for everything that follows. Teams need verified facts, a holding line, and a calm approval process.',
'[{"type":"paragraph","text":"The opening response sets the tone for everything that follows."},{"type":"heading","text":"Establish the facts"},{"type":"paragraph","text":"Teams need verified facts, a holding line, and a calm approval process before any public words leave the building."},{"type":"pullquote","text":"Speed matters, but accuracy is what keeps the second statement from becoming another crisis."},{"type":"paragraph","text":"After the first response, keep a log of facts, decisions, and public commitments."}]',
'["crisis","trust","public affairs"]',
'https://images.unsplash.com/photo-1497366754035-f200968a6e72?auto=format&fit=crop&q=80&w=900',
'https://images.unsplash.com/photo-1497366811353-6870744d04b2?auto=format&fit=crop&q=80&w=1400',
'For You', 'Politics', 6, 121, 533, 14, FALSE, 'PUBLISHED', NOW() - INTERVAL '1 day', NOW(), NOW()
FROM users u WHERE u.email = 'admin@gnsw.ng'
AND NOT EXISTS (SELECT 1 FROM articles a WHERE a.title = 'The First Hour After a Public Crisis');

INSERT INTO articles (id, author_id, title, excerpt, content, tags, thumbnail_url, image_url, tag, category, read_time, claps, views, comment_count, featured, status, published_at, created_at, updated_at)
SELECT gen_random_uuid(), u.id,
'Turning Policy Detail Into Public Meaning',
'Citizens rarely need every technical clause. They need to understand what changed, why it matters, and how it affects daily life.',
'[{"type":"paragraph","text":"Policy communication often fails when it assumes technical accuracy is the same as public understanding."},{"type":"heading","text":"Name the human effect"},{"type":"paragraph","text":"Every policy message should answer three questions: who is affected, what they should expect, and where they can get help."},{"type":"pullquote","text":"People do not reject complexity; they reject being asked to decode it without a guide."},{"type":"paragraph","text":"Good policy language respects the reader. It avoids slogans when precision is needed."}]',
'["policy","translation","citizens"]',
'https://images.unsplash.com/photo-1521791136064-7986c2920216?auto=format&fit=crop&q=80&w=900',
'https://images.unsplash.com/photo-1556761175-b413da4baf72?auto=format&fit=crop&q=80&w=1400',
'Latest', 'Economy', 5, 64, 310, 6, FALSE, 'PUBLISHED', NOW() - INTERVAL '3 hours', NOW(), NOW()
FROM users u WHERE u.email = 'admin@gnsw.ng'
AND NOT EXISTS (SELECT 1 FROM articles a WHERE a.title = 'Turning Policy Detail Into Public Meaning');

INSERT INTO articles (id, author_id, title, excerpt, content, tags, thumbnail_url, image_url, tag, category, read_time, claps, views, comment_count, featured, status, published_at, created_at, updated_at)
SELECT gen_random_uuid(), u.id,
'What Executive Writers Can Learn From Town Halls',
'Town halls reveal the questions people actually care about. Good writers listen for worries, repeated phrases, and the gaps between official language and lived experience.',
'[{"type":"paragraph","text":"Town halls are a live test of institutional language."},{"type":"heading","text":"Listen for repetition"},{"type":"paragraph","text":"When different people ask the same question in different words, the communication gap is real."},{"type":"pullquote","text":"The audience often writes the next draft for you, if you listen carefully enough."},{"type":"paragraph","text":"After a town hall, review the questions before reviewing the applause."}]',
'["town halls","listening","institutions"]',
'https://images.unsplash.com/photo-1540575467063-178a50c2df87?auto=format&fit=crop&q=80&w=900',
'https://images.unsplash.com/photo-1540575467063-178a50c2df87?auto=format&fit=crop&q=80&w=1400',
'Trending', 'Leadership', 7, 150, 640, 21, TRUE, 'PUBLISHED', NOW() - INTERVAL '6 hours', NOW(), NOW()
FROM users u WHERE u.email = 'admin@gnsw.ng'
AND NOT EXISTS (SELECT 1 FROM articles a WHERE a.title = 'What Executive Writers Can Learn From Town Halls');

INSERT INTO articles (id, author_id, title, excerpt, content, tags, thumbnail_url, image_url, tag, category, read_time, claps, views, comment_count, featured, status, published_at, created_at, updated_at)
SELECT gen_random_uuid(), u.id,
'Approval-Ready Drafts for Busy Reviewers',
'Approval-ready drafts reduce friction. They make it easy for reviewers to see the purpose of the piece, the choices made, and the areas where judgment is still needed.',
'[{"type":"paragraph","text":"Approval-ready drafts reduce friction for every reviewer in the chain."},{"type":"heading","text":"Check the essentials"},{"type":"paragraph","text":"Confirm the audience, occasion, speaking time, protocol requirements, names, titles, and sensitivities before sending."},{"type":"pullquote","text":"A clean draft is not just well written. It is easy to trust."},{"type":"paragraph","text":"Before submission, add a short note explaining the main argument and any unresolved questions."}]',
'["drafts","review","process"]',
'https://images.unsplash.com/photo-1455390582262-044cdead277a?auto=format&fit=crop&q=80&w=900',
'https://images.unsplash.com/photo-1455390582262-044cdead277a?auto=format&fit=crop&q=80&w=1400',
'For You', 'Writing', 4, 38, 176, 3, FALSE, 'PUBLISHED', NOW() - INTERVAL '5 hours', NOW(), NOW()
FROM users u WHERE u.email = 'admin@gnsw.ng'
AND NOT EXISTS (SELECT 1 FROM articles a WHERE a.title = 'Approval-Ready Drafts for Busy Reviewers');

-- A submission waiting for admin moderation (visible in the admin Pending Articles screen)
INSERT INTO articles (id, author_id, title, excerpt, content, tags, thumbnail_url, image_url, tag, category, read_time, claps, views, comment_count, featured, status, published_at, created_at, updated_at)
SELECT gen_random_uuid(), u.id,
'The Language of Trust in Public Addresses',
'Trust is built in the small choices: what you name, what you acknowledge, and what you refuse to overstate.',
'[{"type":"paragraph","text":"Trust is built in the small choices a communicator makes."},{"type":"paragraph","text":"Name what is uncertain, acknowledge what changed, and never claim more than the evidence supports."}]',
'["trust","public speaking","ethics"]',
'https://images.unsplash.com/photo-1505664194779-8beaceb93744?auto=format&fit=crop&q=80&w=900',
'https://images.unsplash.com/photo-1505664194779-8beaceb93744?auto=format&fit=crop&q=80&w=1400',
'For You', 'Ethics', 3, 0, 0, 0, FALSE, 'PENDING_REVIEW', NULL, NOW(), NOW()
FROM users u WHERE u.email = 'admin@gnsw.ng'
AND NOT EXISTS (SELECT 1 FROM articles a WHERE a.title = 'The Language of Trust in Public Addresses');
