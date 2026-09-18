-- add reconciled indicator and user notes
ALTER TABLE rails.account_transaction
    ADD COLUMN reconciled boolean NOT NULL DEFAULT FALSE,
    ADD COLUMN notes text NULL;
