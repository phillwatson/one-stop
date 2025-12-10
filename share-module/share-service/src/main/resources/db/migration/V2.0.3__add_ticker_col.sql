ALTER TABLE IF EXISTS shares.share_index
    ALTER COLUMN isin DROP NOT NULL,
    ADD COLUMN ticker_symbol text NULL UNIQUE;

CREATE INDEX IF NOT EXISTS idx_share_ticker ON shares.share_index (ticker_symbol);
