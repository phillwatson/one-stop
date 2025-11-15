-- make portfolio name unique within user's portfolios
ALTER TABLE IF EXISTS shares.portfolio
    DROP CONSTRAINT portfolio_name_key,
    ADD CONSTRAINT portfolio_user_name_unique UNIQUE (user_id, name);
