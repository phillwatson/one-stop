CREATE SCHEMA IF NOT EXISTS ${flyway:defaultSchema};

CREATE TABLE ${flyway:defaultSchema}.userconsent (
    id uuid NOT NULL CONSTRAINT userconsent_pkey PRIMARY KEY,
    rail_provider varchar(256) NOT NULL,
    date_created timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_given timestamp NULL,
    date_denied timestamp NULL,
    date_cancelled timestamp NULL,
    user_id UUID NOT NULL,
    institution_id varchar(256) NOT NULL,
    agreement_id varchar(256) NOT NULL,
    reference varchar(256) NULL,
    agreement_expires timestamp NOT NUlL,
    max_history int NOT NULL,
    callback_uri varchar(256) NULL,
    status varchar(256) NOT NULL,
    error_code varchar(256) NULL,
    error_detail varchar(256) NULL
);
CREATE INDEX idx_userconsent_user_id ON ${flyway:defaultSchema}.userconsent (user_id);
CREATE INDEX idx_userconsent_reference ON ${flyway:defaultSchema}.userconsent (reference);

CREATE TABLE ${flyway:defaultSchema}.account (
	id uuid NOT NULL CONSTRAINT account_pkey PRIMARY KEY,
    user_id UUID NOT NULL,
	userconsent_id UUID NOT NULL CONSTRAINT fk_userconsent REFERENCES ${flyway:defaultSchema}.userconsent (id) ON DELETE CASCADE,
    date_created timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    institution_id varchar(256) NOT NULL,
	rail_account_id varchar(256) NOT NULL,
	iban varchar(256) NULL,
	account_name varchar(256) NULL,
	account_type varchar(256) NULL,
	owner_name varchar(256) NULL,
	currency_code varchar(12) NULL,
	date_last_polled timestamp NULL
);
CREATE INDEX idx_account_rail_id ON ${flyway:defaultSchema}.account (rail_account_id);
CREATE INDEX idx_account_user_id ON ${flyway:defaultSchema}.account (user_id);

CREATE TABLE ${flyway:defaultSchema}.account_balance (
	id uuid NOT NULL CONSTRAINT balance_pkey PRIMARY KEY,
	account_id uuid NOT NULL CONSTRAINT fk_account REFERENCES ${flyway:defaultSchema}.account (id) ON DELETE CASCADE,
	date_created timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    amount bigint NOT NULL,
    currency_code varchar(12) NOT NULL,
    balance_type varchar(256) NULL,
    reference_date date NULL,
    last_committed_transaction varchar(256) NULL
);
CREATE INDEX idx_account_balance_date ON ${flyway:defaultSchema}.account_balance (account_id, date_created);

CREATE TABLE ${flyway:defaultSchema}.account_transaction (
	id uuid NOT NULL CONSTRAINT transaction_pkey PRIMARY KEY,
    user_id UUID NOT NULL,
	account_id uuid NOT NULL CONSTRAINT fk_account REFERENCES ${flyway:defaultSchema}.account (id) ON DELETE CASCADE,
	date_created timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    internal_transaction_id varchar(256) NOT NULL,
    transaction_id varchar(256) NULL,
    booking_datetime timestamp NOT NULL,
    value_datetime timestamp NULL,
    amount bigint NOT NULL,
    currency_code varchar(12) NOT NULL,
    additional_information varchar(1024) NULL,
    creditor_name varchar(256) NULL,
    reference varchar(256) NULL
);
CREATE INDEX idx_account_trans_date ON ${flyway:defaultSchema}.account_transaction (account_id, booking_datetime);
CREATE INDEX idx_account_user_date ON ${flyway:defaultSchema}.account_transaction (user_id, booking_datetime);
CREATE INDEX idx_account_intrnl_id ON ${flyway:defaultSchema}.account_transaction (internal_transaction_id);
