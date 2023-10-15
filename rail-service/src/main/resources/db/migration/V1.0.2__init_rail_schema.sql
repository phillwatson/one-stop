CREATE SCHEMA IF NOT EXISTS rails;

CREATE TABLE IF NOT EXISTS rails.userconsent (
    id uuid NOT NULL CONSTRAINT userconsent_pkey PRIMARY KEY,
    date_created timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_given timestamp NULL,
    date_denied timestamp NULL,
    date_cancelled timestamp NULL,
    user_id UUID NOT NULL,
    institution_id varchar(256) NOT NULL,
    agreement_id varchar(256) NOT NULL,
    agreement_expires timestamp NOT NUlL,
    max_history int NOT NULL,
    requisition_id varchar(256) NULL,
    callback_uri varchar(256) NULL,
    status varchar(256) NOT NULL,
    error_code varchar(256) NULL,
    error_detail varchar(256) NULL
);
CREATE INDEX idx_userconsent_user_id ON rails.userconsent (user_id);

CREATE TABLE IF NOT EXISTS rails.account (
	id uuid NOT NULL CONSTRAINT account_pkey PRIMARY KEY,
    user_id UUID NOT NULL,
	userconsent_id UUID NOT NULL CONSTRAINT fk_userconsent REFERENCES rails.userconsent (id) ON DELETE CASCADE,
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
CREATE INDEX idx_account_rail_id ON rails.account (rail_account_id);
CREATE INDEX idx_account_user_id ON rails.account (user_id);

CREATE TABLE IF NOT EXISTS rails.account_balance (
	id uuid NOT NULL CONSTRAINT balance_pkey PRIMARY KEY,
	account_id uuid NOT NULL CONSTRAINT fk_account REFERENCES rails.account (id) ON DELETE CASCADE,
	date_created timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    amount numeric(19,2) NOT NULL,
    currency_code varchar(12) NOT NULL,
    balance_type varchar(256) NULL,
    reference_date date NULL,
    last_committed_transaction varchar(256) NULL
);
CREATE INDEX idx_account_balance_date ON rails.account_balance (account_id, date_created);

CREATE TABLE IF NOT EXISTS rails.account_transaction (
	id uuid NOT NULL CONSTRAINT transaction_pkey PRIMARY KEY,
    user_id UUID NOT NULL,
	account_id uuid NOT NULL CONSTRAINT fk_account REFERENCES rails.account (id) ON DELETE CASCADE,
	date_created timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    internal_transaction_id varchar(256) NOT NULL,
    transaction_id varchar(256) NULL,
    booking_datetime timestamp NOT NULL,
    value_datetime timestamp NULL,
    transaction_amount numeric(19,2) NOT NULL,
    transaction_currency_code varchar(12) NOT NULL,
    remittance_information_structured varchar(1024) NULL,
    remittance_information_unstructured varchar(1024) NULL,
    remittance_information_structured_array varchar(1024) NULL,
    remittance_information_unstructured_array varchar(1024) NULL,
    additional_information varchar(1024) NULL,
    additional_information_structured varchar(1024) NULL,
    balance_after_transaction varchar(256) NULL,
    bank_transaction_code varchar(256) NULL,
    check_id varchar(256) NULL,
    creditor_iban varchar(256) NULL,
    creditor_agent varchar(256) NULL,
    creditor_id varchar(256) NULL,
    creditor_name varchar(256) NULL,
    currency_exchange varchar(256) NULL,
    debtor_iban varchar(256) NULL,
    debtor_agent varchar(256) NULL,
    debtor_name varchar(256) NULL,
    end_to_end_id varchar(256) NULL,
    entry_reference varchar(256) NULL,
    mandate_id varchar(256) NULL,
    merchant_category_code varchar(256) NULL,
    proprietary_bank_transaction_code varchar(256) NULL,
    purpose_code varchar(256) NULL,
    ultimate_creditor varchar(256) NULL,
    ultimate_debtor varchar(256) NULL
);
CREATE INDEX idx_account_trans_date ON rails.account_transaction (account_id, booking_datetime);
CREATE INDEX idx_account_user_date ON rails.account_transaction (user_id, booking_datetime);
CREATE INDEX idx_account_intrnl_id ON rails.account_transaction (internal_transaction_id);
