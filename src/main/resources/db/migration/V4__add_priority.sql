ALTER TABLE crawl_queue ADD COLUMN priority INT NOT NULL DEFAULT 0;

CREATE INDEX idx_crawl_queue_pending_order ON crawl_queue (status, priority DESC, depth ASC);
