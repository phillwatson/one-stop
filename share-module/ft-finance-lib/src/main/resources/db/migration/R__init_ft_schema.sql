CREATE SCHEMA IF NOT EXISTS ${flyway:defaultSchema};

-- used to look-up FT Finance issue-IDs using their ISIN value
CREATE TABLE IF NOT EXISTS ${flyway:defaultSchema}.isin_issue_lookup (
    isin varchar(256) NOT NULL UNIQUE PRIMARY KEY,
    issue_id varchar(256),
    name varchar(256) NULL,
    currency_code varchar(12)
);
