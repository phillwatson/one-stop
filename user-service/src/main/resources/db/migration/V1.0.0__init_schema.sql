CREATE SCHEMA IF NOT EXISTS ${flyway:defaultSchema};

CREATE TABLE IF NOT EXISTS ${flyway:defaultSchema}.user (
    id uuid PRIMARY KEY,
    username varchar(256) NOT NULL UNIQUE,
    password_hash varchar(256) NOT NULL,
    email varchar(256) NOT NULL,
    title varchar(256),
    given_name varchar(256) NOT NULL,
    family_name varchar(256),
    preferred_name varchar(256),
    phone_number varchar(256),
    date_created timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_onboarded timestamp,
    date_deleted timestamp,
    date_blocked timestamp,
    blocked_reason varchar(256),
    version integer NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS ${flyway:defaultSchema}.userrole (
    user_id uuid NOT NULL,
    role varchar(256) NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_userrole ON ${flyway:defaultSchema}.userrole (user_id, role);

INSERT INTO ${flyway:defaultSchema}.user (id, username, email, given_name, password_hash)
VALUES ('9abc5177-421d-4913-a1e4-b5f69ca1ae93', 'admin', 'watson.phill+onestop@gmail.com', 'Admin', '1000:1284d970625dbdd0103f51a06f33a6c6:460f368479614f24c19bf498733186e88ad933453c8f5087154cd53eed39b50d1c295ef1ea3bdec417fa106cc319db008fe1fca2a2fc7d6e46e1ad6f952e30e8');

INSERT INTO ${flyway:defaultSchema}.userrole (user_id, role) VALUES ('9abc5177-421d-4913-a1e4-b5f69ca1ae93', 'admin');
