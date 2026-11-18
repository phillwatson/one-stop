CREATE TABLE rails.category (
    id uuid NOT NULL CONSTRAINT category_pkey PRIMARY KEY,
    version bigint NOT NULL DEFAULT 0,
    user_id UUID NOT NULL,
    name varchar(256) NOT NULL,
    description varchar(256) NULL,
    colour varchar(256) NULL
);
CREATE UNIQUE INDEX idx_category_user_id ON rails.category (user_id, name);

CREATE TABLE rails.category_selector (
    id uuid NOT NULL CONSTRAINT category_selector_pkey PRIMARY KEY,
    category_id uuid NOT NULL CONSTRAINT fk_category REFERENCES rails.category (id) ON DELETE CASCADE,
    account_id uuid NOT NULL CONSTRAINT fk_account REFERENCES rails.account (id) ON DELETE CASCADE,
    info_contains varchar(256) NULL,
    ref_contains varchar(256) NULL,
    creditor_contains varchar(256) NULL
);
CREATE INDEX idx_category_selector_category_id ON rails.category_selector (category_id);
CREATE INDEX idx_category_selector_account_id ON rails.category_selector (account_id);
