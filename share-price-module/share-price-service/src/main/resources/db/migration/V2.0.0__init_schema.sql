CREATE SCHEMA IF NOT EXISTS ${flyway:defaultSchema};

-- records the shares that have been registered by any user
CREATE TABLE IF NOT EXISTS ${flyway:defaultSchema}.share_index (
    id uuid PRIMARY KEY,
    isin varchar(256) NOT NULL,
    name varchar(256) NOT NULL,
    currency_code varchar(12) NOT NULL,
    provider varchar(256) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_share_isin ON ${flyway:defaultSchema}.share_index (isin);
CREATE INDEX IF NOT EXISTS idx_share_name ON ${flyway:defaultSchema}.share_index (name);

-- records the history of a user's
CREATE TABLE IF NOT EXISTS ${flyway:defaultSchema}.share_price_history (
	share_index_id UUID NOT NULL CONSTRAINT fk_history_share_index REFERENCES ${flyway:defaultSchema}.share_index (id) ON DELETE CASCADE,
	resolution varchar(256) NOT NULL,
    market_date date NOT NULL,
    open_price numeric(16, 4) NOT NULL,
    high_price numeric(16, 4) NOT NULL,
    low_price numeric(16, 4) NOT NULL,
    close_price numeric(16, 4) NOT NULL,
    PRIMARY KEY (share_index_id, market_date)
);

-- records the shares in which a user has holdings
CREATE TABLE IF NOT EXISTS ${flyway:defaultSchema}.share_holding (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL,
    share_index_id UUID NOT NULL CONSTRAINT fk_share_index REFERENCES ${flyway:defaultSchema}.share_index (id) ON DELETE CASCADE,
    date_created timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_user_share_holding ON ${flyway:defaultSchema}.share_holding (user_id, share_index_id);

-- records a user's dealings against a share holding
CREATE TABLE IF NOT EXISTS ${flyway:defaultSchema}.dealing_history (
    id uuid PRIMARY KEY,
	share_holding_id UUID NOT NULL CONSTRAINT fk_dealing_share_holding REFERENCES ${flyway:defaultSchema}.share_holding (id) ON DELETE CASCADE,
	market_date date NOT NULL,
	is_purchase boolean NOT NULL DEFAULT true,
	quantity integer NOT NULL,
	price numeric(16, 4) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_share_dealing ON ${flyway:defaultSchema}.dealing_history (share_holding_id, market_date);
