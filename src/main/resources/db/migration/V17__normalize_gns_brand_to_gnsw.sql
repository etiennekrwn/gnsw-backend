-- V17: Normalize residual legacy 'GNS' (3-letter) brand tokens that were stored in the
--      database before the gnsw revert, so live rows match the GNSW-wide codebase.
--      Idempotent + collision-safe: each UPDATE is guarded so it is a pure no-op on a
--      database that is already fully 'GNSW' (e.g. a fresh deployment whose V11 default
--      and AdminSettingsController seed now write 'GNSW ...' values). Run by Flyway on
--      the next deployment. No schema changes, only data normalization.

-- 1) Guild-wide settings that were seeded with the old 'GNS' prefix.
UPDATE guild_settings
   SET portal_name = 'GNSW Members Portal'
 WHERE portal_name = 'GNS Members Portal';

UPDATE guild_settings
   SET id_prefix = 'GNSW'
 WHERE id_prefix = 'GNS';

-- 2) Member professional IDs 'GNS-YYYY-NNN' -> 'GNSW-YYYY-NNN'.
--    Safe under the UNIQUE(proessional_id) constraint:
--      * LIKE 'GNS-%' does NOT match 'GNSW-*' rows (the 4th char differs: '-' vs 'W'),
--        so already-correct GNSW ids are never touched.
--      * The NOT EXISTS guard skips any id whose target 'GNSW-*' value is already taken,
--        preventing a uniqueness violation on a partially-migrated database.
UPDATE users u
   SET professional_id = 'GNSW-' || substr(u.professional_id, 5)
 WHERE u.professional_id LIKE 'GNS-%'
   AND NOT EXISTS (
     SELECT 1 FROM users u2
      WHERE u2.id <> u.id
        AND u2.professional_id = ('GNSW-' || substr(u.professional_id, 5))
   );
