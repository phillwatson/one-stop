CREATE SCHEMA IF NOT EXISTS ${flyway:defaultSchema};

CREATE TABLE IF NOT EXISTS ${flyway:defaultSchema}.user (
    id uuid PRIMARY KEY,
    username varchar(256) NOT NULL,
    email varchar(256) NOT NULL,
    given_name varchar(256),
    family_name varchar(256),
    version integer NOT NULL DEFAULT 0
);
