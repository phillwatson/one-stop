-- a table to hold the user's audit report configurations
CREATE TABLE ${flyway:defaultSchema}.audit_report_config (
    id uuid NOT NULL CONSTRAINT audit_report_config_pkey PRIMARY KEY,
    disabled boolean NOT NULL DEFAULT FALSE,
    user_id UUID NOT NULL,
    name varchar(256) NOT NULL,
    description varchar(256) NULL,
    report_source varchar(256) NOT NULL,
    report_source_id UUID NULL,
    include_uncategorised boolean NOT NULL DEFAULT FALSE,
    template_id varchar(256) NOT NULL,
    version bigint NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX idx_audit_report_config_name ON ${flyway:defaultSchema}.audit_report_config (user_id, name);
CREATE INDEX idx_report_source_id ON ${flyway:defaultSchema}.audit_report_config (report_source_id);

-- a table to hold the parameters for the user's audit report configurations
CREATE TABLE ${flyway:defaultSchema}.audit_report_parameter (
    id uuid NOT NULL CONSTRAINT audit_report_param_pkey PRIMARY KEY,
    report_config_id UUID NOT NULL CONSTRAINT fk_audit_report_config REFERENCES ${flyway:defaultSchema}.audit_report_config (id) ON DELETE CASCADE,
    param_name varchar(256) NOT NULL,
    param_value varchar(256) NULL
);
CREATE INDEX idx_audit_report_config ON ${flyway:defaultSchema}.audit_report_parameter (report_config_id);
