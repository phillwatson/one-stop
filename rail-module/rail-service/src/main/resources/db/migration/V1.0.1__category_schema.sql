CREATE SCHEMA IF NOT EXISTS ${flyway:defaultSchema};

CREATE TABLE ${flyway:defaultSchema}.category (
    id uuid NOT NULL CONSTRAINT category_pkey PRIMARY KEY,
    version bigint NOT NULL DEFAULT 0,
    user_id UUID NOT NULL,
    parent_id uuid NULL,
    name varchar(256) NOT NULL,
    description varchar(256) NULL,
    colour varchar(256) NULL
);
CREATE INDEX idx_category_user_id ON ${flyway:defaultSchema}.category (user_id);
CREATE UNIQUE INDEX idx_category_parent_id ON ${flyway:defaultSchema}.category (parent_id, name);

CREATE TABLE ${flyway:defaultSchema}.category_selector (
    id uuid NOT NULL CONSTRAINT category_selector_pkey PRIMARY KEY,
    version bigint NOT NULL DEFAULT 0,
    account_id uuid NOT NULL CONSTRAINT fk_account REFERENCES ${flyway:defaultSchema}.account (id) ON DELETE CASCADE,
    category_id uuid NOT NULL CONSTRAINT fk_category REFERENCES ${flyway:defaultSchema}.category (id) ON DELETE CASCADE,
    regex varchar(256) NOT NULL
);
CREATE INDEX idx_category_selector_account_id ON ${flyway:defaultSchema}.category_selector (account_id, category_id);
