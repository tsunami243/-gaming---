-- Promote the Delta Force official source from FETCH_VERIFIED to SHADOW so the collection
-- worker may actually run it. SHADOW is still fully gated: every ingested article lands as
-- PENDING_REVIEW and needs a human approval, so this does not enable automatic publication.
-- auto_publish_enabled stays 0, and a deliberately paused source stays paused.
UPDATE content_source
SET verification_status = 'SHADOW',
    auto_publish_enabled = 0
WHERE code = 'delta-force-garena-official'
  AND verification_status <> 'PAUSED';
