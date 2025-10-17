CREATE SCHEMA IF NOT EXISTS ${flyway:defaultSchema};

-- records the shares that have been registered by any user
CREATE TABLE IF NOT EXISTS ${flyway:defaultSchema}.share_index (
    id uuid PRIMARY KEY,
    isin text NOT NULL,
    name text NOT NULL,
    currency_code text NOT NULL,
    provider text NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_share_isin ON ${flyway:defaultSchema}.share_index (isin);
CREATE INDEX IF NOT EXISTS idx_share_name ON ${flyway:defaultSchema}.share_index (name);


-- records the history of a user's
CREATE TABLE IF NOT EXISTS ${flyway:defaultSchema}.price_history (
	share_index_id UUID NOT NULL CONSTRAINT fk_history_share_index REFERENCES ${flyway:defaultSchema}.share_index (id) ON DELETE CASCADE,
	resolution text NOT NULL,
    market_date date NOT NULL,
    open_price numeric(16, 4) NOT NULL,
    high_price numeric(16, 4) NOT NULL,
    low_price numeric(16, 4) NOT NULL,
    close_price numeric(16, 4) NOT NULL,
    PRIMARY KEY (share_index_id, market_date)
);


-- records the a user's portfolio of shares
CREATE TABLE IF NOT EXISTS ${flyway:defaultSchema}.portfolio (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL,
    name text NOT NULL,
    date_created timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_user_portfolio ON ${flyway:defaultSchema}.portfolio (user_id);


-- records the shares in which a user has within a portfolio
CREATE TABLE IF NOT EXISTS ${flyway:defaultSchema}.holding (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL,
    portfolio_id UUID NOT NULL CONSTRAINT fk_portfolio REFERENCES ${flyway:defaultSchema}.portfolio (id) ON DELETE CASCADE,
    share_index_id UUID NOT NULL CONSTRAINT fk_share_index REFERENCES ${flyway:defaultSchema}.share_index (id) ON DELETE CASCADE,
    date_created timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_user_holding ON ${flyway:defaultSchema}.holding (user_id, share_index_id);
CREATE INDEX IF NOT EXISTS idx_portfolio_holding ON ${flyway:defaultSchema}.holding (portfolio_id, share_index_id);


-- records a user's dealings against a share holding
CREATE TABLE IF NOT EXISTS ${flyway:defaultSchema}.dealing_history (
    id uuid PRIMARY KEY,
	holding_id UUID NOT NULL CONSTRAINT fk_dealing_holding REFERENCES ${flyway:defaultSchema}.holding (id) ON DELETE CASCADE,
	market_date date NOT NULL,
	quantity integer NOT NULL, -- the number of shares bought (positive) or sold (negative)
	price numeric(16, 4) NOT NULL -- the price of each share (always positive)
);
CREATE INDEX IF NOT EXISTS idx_share_dealing ON ${flyway:defaultSchema}.dealing_history (holding_id, market_date);
