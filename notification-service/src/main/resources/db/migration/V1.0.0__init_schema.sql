CREATE SCHEMA IF NOT EXISTS ${flyway:defaultSchema};

CREATE TABLE IF NOT EXISTS ${flyway:defaultSchema}.user (
    id uuid PRIMARY KEY,
    username varchar(256) NOT NULL,
    email varchar(256) NOT NULL,
    title varchar(256),
    given_name varchar(256) NOT NULL,
    family_name varchar(256),
    preferred_name varchar(256),
    phone_number varchar(256),
    locales varchar(256),
    date_created timestamp NULL,
    date_updated timestamp NULL,
    version integer NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS ${flyway:defaultSchema}.notification (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL,
    date_created timestamp NOT NULL,
    message_id varchar(256) NOT NULl,
    attributes text NULL
);
CREATE INDEX idx_notification_user_id ON ${flyway:defaultSchema}.notification (user_id, date_created);
