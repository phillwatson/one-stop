ALTER TABLE IF EXISTS shares.dealing_history
    RENAME COLUMN market_date TO date_executed;

DROP INDEX IF EXISTS idx_user_holding;
DROP INDEX IF EXISTS idx_portfolio_holding;

ALTER TABLE IF EXISTS shares.holding
    DROP COLUMN IF EXISTS user_id,
    ADD CONSTRAINT uq_portfolio_share UNIQUE (portfolio_id, share_index_id);
