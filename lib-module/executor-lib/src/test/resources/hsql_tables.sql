

CREATE TABLE IF NOT EXISTS scheduled_tasks (
  task_name varchar(100) NOT NULL,
  task_instance varchar(100) NOT NULL,
  task_data blob,
  execution_time TIMESTAMP WITH TIME ZONE NOT NULL,
  picked BIT NOT NULL,
  picked_by varchar(50),
  last_success TIMESTAMP WITH TIME ZONE,
  last_failure TIMESTAMP WITH TIME ZONE,
  consecutive_failures INT,
  last_heartbeat TIMESTAMP WITH TIME ZONE,
  version BIGINT NOT NULL,
  PRIMARY KEY (task_name, task_instance)
)
