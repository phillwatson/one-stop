CREATE SCHEMA IF NOT EXISTS shares;

-- records the shares that have been registered by any user
CREATE TABLE IF NOT EXISTS shares.share_index (
    id uuid PRIMARY KEY,
    isin text NOT NULL UNIQUE,
    name text NOT NULL,
    currency_code text NOT NULL,
    provider text NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_share_isin ON shares.share_index (isin);
CREATE INDEX IF NOT EXISTS idx_share_name ON shares.share_index (name);


-- records the history of a user's
CREATE TABLE IF NOT EXISTS shares.price_history (
	share_index_id UUID NOT NULL CONSTRAINT fk_history_share_index REFERENCES shares.share_index (id) ON DELETE CASCADE,
	resolution text NOT NULL,
    market_date date NOT NULL,
    open_price numeric(16, 4) NOT NULL,
    high_price numeric(16, 4) NOT NULL,
    low_price numeric(16, 4) NOT NULL,
    close_price numeric(16, 4) NOT NULL,
    volume bigint NOT NULL,
    PRIMARY KEY (share_index_id, resolution, market_date)
);


-- records the a user's portfolio of shares
CREATE TABLE IF NOT EXISTS shares.portfolio (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL,
    name text NOT NULL UNIQUE,
    date_created timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_user_portfolio ON shares.portfolio (user_id);


-- records the shares in which a user has within a portfolio
CREATE TABLE IF NOT EXISTS shares.holding (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL,
    portfolio_id UUID NOT NULL CONSTRAINT fk_portfolio REFERENCES shares.portfolio (id) ON DELETE CASCADE,
    share_index_id UUID NOT NULL CONSTRAINT fk_share_index REFERENCES shares.share_index (id) ON DELETE CASCADE,
    date_created timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_user_holding ON shares.holding (user_id, share_index_id);
CREATE INDEX IF NOT EXISTS idx_portfolio_holding ON shares.holding (portfolio_id, share_index_id);


-- records a user's dealings against a share holding
CREATE TABLE IF NOT EXISTS shares.dealing_history (
    id uuid PRIMARY KEY,
	holding_id UUID NOT NULL CONSTRAINT fk_dealing_holding REFERENCES shares.holding (id) ON DELETE CASCADE,
	market_date date NOT NULL,
	quantity integer NOT NULL, -- the number of shares bought (positive) or sold (negative)
	price numeric(16, 4) NOT NULL -- the price of each share (always positive)
);
CREATE INDEX IF NOT EXISTS idx_share_dealing ON shares.dealing_history (holding_id, market_date);
