CREATE SCHEMA IF NOT EXISTS ${flyway:defaultSchema};

CREATE TABLE IF NOT EXISTS ${flyway:defaultSchema}.user (
    id uuid PRIMARY KEY,
    username varchar(256) NOT NULL UNIQUE,
    password_hash varchar(256) NOT NULL,
    email varchar(256) NOT NULL UNIQUE,
    given_name varchar(256),
    family_name varchar(256),
    phone_number varchar(256),
    date_created timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_onboarded timestamp,
    version integer
)
