CREATE SCHEMA IF NOT EXISTS ${flyway:defaultSchema};

-- used to look-up FT Finance issue-IDs using their ISIN value
CREATE TABLE IF NOT EXISTS ${flyway:defaultSchema}.isin_issue_lookup (
    isin varchar(256) NOT NULL UNIQUE PRIMARY KEY,
    issue_id varchar(256),
    name varchar(256) NULL,
    currency_code varchar(12)
);

-- add column to indicate the units in which prices are delivered
ALTER TABLE IF EXISTS ${flyway:defaultSchema}.isin_issue_lookup
    ADD COLUMN IF NOT EXISTS currency_units varchar(20) NOT NULL DEFAULT 'MINOR';
