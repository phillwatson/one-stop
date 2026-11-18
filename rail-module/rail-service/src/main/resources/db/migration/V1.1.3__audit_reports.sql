-- a table to hold the user's audit report configurations
CREATE TABLE rails.audit_report_config (
    id uuid NOT NULL CONSTRAINT audit_report_config_pkey PRIMARY KEY,
    disabled boolean NOT NULL DEFAULT FALSE,
    user_id UUID NOT NULL,
    name varchar(256) NOT NULL,
    description varchar(256) NULL,
    report_source varchar(256) NOT NULL,
    report_source_id UUID NULL,
    include_uncategorised boolean NOT NULL DEFAULT FALSE,
    template_name varchar(256) NOT NULL,
    version bigint NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX idx_audit_report_config_name ON rails.audit_report_config (user_id, name);
CREATE INDEX idx_report_source_id ON rails.audit_report_config (report_source_id);

-- a table to hold the parameters for the user's audit report configurations
CREATE TABLE rails.audit_report_parameter (
    id uuid NOT NULL CONSTRAINT audit_report_param_pkey PRIMARY KEY,
    report_config_id UUID NOT NULL CONSTRAINT fk_audit_report_config REFERENCES rails.audit_report_config (id) ON DELETE CASCADE,
    param_name varchar(256) NOT NULL,
    param_value varchar(256) NULL
);
CREATE INDEX idx_audit_report_config ON rails.audit_report_parameter (report_config_id);

-- a table to hold the transactions discovered in user audit reports
CREATE TABLE rails.audit_issue (
    id uuid NOT NULL CONSTRAINT audit_issue_pkey PRIMARY KEY,
    user_id UUID NOT NULL,
    report_config_id UUID NOT NULL CONSTRAINT fk_audit_issue_report_config REFERENCES rails.audit_report_config (id) ON DELETE CASCADE,
    transaction_id UUID NOT NULL CONSTRAINT fk_audit_issue_transaction REFERENCES rails.account_transaction (id) ON DELETE CASCADE,
    booking_datetime timestamp NOT NULL,
    acknowledged boolean NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_audit_issue_user ON rails.audit_issue (user_id);
CREATE INDEX idx_audit_issue_report_config ON rails.audit_issue (report_config_id);
CREATE INDEX idx_audit_issue_transaction ON rails.audit_issue (transaction_id);
