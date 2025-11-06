ALTER TABLE IF EXISTS ${flyway:defaultSchema}.share_index
    ALTER COLUMN isin DROP NOT NULL,
    ADD COLUMN ticker_symbol text NULL UNIQUE;

CREATE INDEX IF NOT EXISTS idx_share_ticker ON ${flyway:defaultSchema}.share_index (ticker_symbol);
