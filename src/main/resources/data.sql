-- Admin seed user - Run this ONCE after tables are created
-- Password: Admin@12345 (BCrypt hashed)
-- IMPORTANT: Change this password after first login!

INSERT INTO users (id, email, first_name, last_name, address_line1, city, state_province, zip_postal_code, country, tier, status, role, password_hash, username, created_at, updated_at)
VALUES (gen_random_uuid(), 'admin@gnsw.ng', 'Super', 'Admin', 'GNSW HQ', 'Lagos', 'Lagos State', '100001', 'Nigeria', 'FELLOW', 'ACCEPTED', 'ROLE_ADMIN',
'$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'admin', NOW(), NOW())
ON CONFLICT (email) DO NOTHING;