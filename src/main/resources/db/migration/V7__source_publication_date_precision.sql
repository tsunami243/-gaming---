-- Preserve date-only source evidence without inventing time-of-day or offset.
ALTER TABLE article ADD COLUMN original_published_date DATE NULL AFTER original_published_at;
