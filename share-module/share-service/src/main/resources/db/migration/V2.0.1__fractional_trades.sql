-- allow for fractional share trade quantities
ALTER TABLE ${flyway:defaultSchema}.share_trade
    ALTER COLUMN quantity TYPE numeric(16, 4);
