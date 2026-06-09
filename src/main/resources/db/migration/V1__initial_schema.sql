CREATE TABLE page (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    url         TEXT        NOT NULL,
    url_hash    VARCHAR(64)    NOT NULL,
    title       TEXT,
    content     TEXT,
    content_hash VARCHAR(64),
    status      VARCHAR(32) NOT NULL DEFAULT 'FETCHED',
    fetched_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_page_url_hash UNIQUE (url_hash)
);

CREATE TABLE crawl_queue (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    url         TEXT        NOT NULL,
    url_hash    VARCHAR(64)    NOT NULL,
    depth       INT         NOT NULL DEFAULT 0,
    status      VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_crawl_queue_url_hash UNIQUE (url_hash)
);

CREATE INDEX idx_page_status       ON page (status);
CREATE INDEX idx_page_fetched_at   ON page (fetched_at);
CREATE INDEX idx_crawl_queue_status ON crawl_queue (status);
