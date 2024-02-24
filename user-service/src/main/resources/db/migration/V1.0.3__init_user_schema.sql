CREATE SCHEMA IF NOT EXISTS users;

-- used to store users
CREATE TABLE IF NOT EXISTS users.user (
    id uuid PRIMARY KEY,
    username varchar(256) NOT NULL UNIQUE,
    password_hash varchar(256),
    password_last_set timestamp,
    email varchar(256),
    title varchar(256),
    given_name varchar(256) NOT NULL,
    family_name varchar(256),
    preferred_name varchar(256),
    phone_number varchar(256),
    locale varchar(256),
    date_created timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_onboarded timestamp,
    date_blocked timestamp,
    blocked_reason varchar(256),
    version integer NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_useremail ON users.user (email);

-- used to store user roles
CREATE TABLE IF NOT EXISTS users.userrole (
    user_id uuid NOT NULL REFERENCES users.user(id) ON DELETE CASCADE,
    role varchar(256) NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_userrole ON users.userrole (user_id, role);

-- create an admin user
INSERT INTO users.user (id, username, email, given_name, password_hash)
VALUES ('9abc5177-421d-4913-a1e4-b5f69ca1ae93', 'admin', 'watson.phill+onestop@gmail.com', 'Admin', '1000:1284d970625dbdd0103f51a06f33a6c6:460f368479614f24c19bf498733186e88ad933453c8f5087154cd53eed39b50d1c295ef1ea3bdec417fa106cc319db008fe1fca2a2fc7d6e46e1ad6f952e30e8');
INSERT INTO users.userrole (user_id, role) VALUES ('9abc5177-421d-4913-a1e4-b5f69ca1ae93', 'admin');


-- used to store identities from external Open-ID Connect providers
CREATE TABLE IF NOT EXISTS users.oidcidentity (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES users.user(id) ON DELETE CASCADE,
    provider varchar(256) NOT NULL,
    issuer varchar(256) NOT NULL,
    subject varchar(256) NOT NULL,
    date_created timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_disabled timestamp NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_oidcissuer ON users.oidcidentity (user_id, issuer);

-- used to store deleted users for audit purposes
CREATE TABLE IF NOT EXISTS users.deleted_user (
    id uuid PRIMARY KEY,
    username varchar(256) NOT NULL,
    password_hash varchar(256),
    email varchar(256),
    title varchar(256),
    given_name varchar(256) NOT NULL,
    family_name varchar(256),
    preferred_name varchar(256),
    phone_number varchar(256),
    locale varchar(256),
    date_created timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_onboarded timestamp,
    date_deleted timestamp,
    date_blocked timestamp,
    blocked_reason varchar(256),
    version integer NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_deluseremail ON users.deleted_user (email);
