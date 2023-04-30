CREATE SCHEMA IF NOT EXISTS ${flyway:defaultSchema};

CREATE TABLE IF NOT EXISTS ${flyway:defaultSchema}.events (
  id uuid PRIMARY KEY,
  correlation_id varchar(256) NOT NULL,
  retry_count smallint NOT NULL DEFAULT 0,
  "timestamp" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  scheduled_for timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  topic varchar(256) NOT NULL,
  key varchar(256) NULL,
  payload_class text NOT NULL,
  payload text NOT NULL
);

CREATE TABLE IF NOT EXISTS ${flyway:defaultSchema}.message_hospital (
  id uuid PRIMARY KEY,
  correlation_id varchar(256) NOT NULL,
  retry_count smallint NOT NULL DEFAULT 0,
  "timestamp" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  reason text NOT NULL,
  cause text NOT NULL,
  topic varchar(256) NOT NULL,
  key varchar(256) NULL,
  payload_class text NOT NULL,
  payload text NOT NULL
);
