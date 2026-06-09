CREATE INDEX idx_page_content_hash ON page (content_hash);
CREATE INDEX idx_crawl_queue_depth_status ON crawl_queue (depth, status);
