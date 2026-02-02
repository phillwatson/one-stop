CREATE SCHEMA IF NOT EXISTS ${flyway:defaultSchema};

-- records the shares that have been registered by any user
CREATE TABLE IF NOT EXISTS ${flyway:defaultSchema}.share_index (
    id uuid PRIMARY KEY,
    isin text NOT NULL UNIQUE,
    ticker_symbol text NULL UNIQUE,
    name text NOT NULL,
    currency_code text NOT NULL,
    provider text NOT NULL
);
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
    volume bigint NOT NULL,
    PRIMARY KEY (share_index_id, resolution, market_date)
);

-- records the a user's portfolio of shares
CREATE TABLE IF NOT EXISTS ${flyway:defaultSchema}.portfolio (
     id uuid PRIMARY KEY,
     user_id uuid NOT NULL,
     name text NOT NULL,
     date_created timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
     CONSTRAINT portfolio_user_name_unique UNIQUE (user_id, name)
);
CREATE INDEX IF NOT EXISTS idx_user_portfolio ON ${flyway:defaultSchema}.portfolio (user_id);

-- records a user's share dealings within a portfolio
CREATE TABLE IF NOT EXISTS ${flyway:defaultSchema}.share_trade (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL,
    portfolio_id UUID NOT NULL CONSTRAINT fk_trade_portfolio REFERENCES ${flyway:defaultSchema}.portfolio (id) ON DELETE CASCADE,
    share_index_id UUID NOT NULL CONSTRAINT fk_trade_share_index REFERENCES ${flyway:defaultSchema}.share_index (id) ON DELETE CASCADE,
    date_executed date NOT NULL,
    quantity integer NOT NULL, -- the number of shares bought (positive) or sold (negative)
    price numeric(16, 4) NOT NULL -- the price of each share (always positive)
);
CREATE INDEX IF NOT EXISTS idx_user_trade ON ${flyway:defaultSchema}.share_trade (user_id, portfolio_id, share_index_id, date_executed);
