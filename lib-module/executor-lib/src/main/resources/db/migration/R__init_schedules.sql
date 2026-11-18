CREATE SCHEMA IF NOT EXISTS ${flyway:defaultSchema};

CREATE TABLE IF NOT EXISTS ${flyway:defaultSchema}.scheduled_tasks (
  task_name text NOT NULL,
  task_instance text NOT NULL,
  task_data bytea,
  execution_time timestamp with time zone NOT NULL,
  picked BOOLEAN NOT NULL,
  picked_by text,
  last_success timestamp with time zone,
  last_failure timestamp with time zone,
  consecutive_failures INT,
  last_heartbeat timestamp with time zone,
  version BIGINT NOT NULL,
  PRIMARY KEY (task_name, task_instance)
);

CREATE INDEX IF NOT EXISTS execution_time_idx ON ${flyway:defaultSchema}.scheduled_tasks (execution_time);
CREATE INDEX IF NOT EXISTS last_heartbeat_idx ON ${flyway:defaultSchema}.scheduled_tasks (last_heartbeat);
