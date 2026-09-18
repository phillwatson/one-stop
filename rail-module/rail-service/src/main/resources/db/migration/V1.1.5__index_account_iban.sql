-- replace user-id index with one that includes the IBAN
DROP INDEX rails.idx_account_user_id CASCADE;
CREATE INDEX idx_account_iban ON rails.account (user_id, iban);
