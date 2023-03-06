CREATE SCHEMA IF NOT EXISTS ${flyway:defaultSchema};

CREATE TABLE ${flyway:defaultSchema}.userconsent (
    id uuid NOT NULL CONSTRAINT userconsent_pkey PRIMARY KEY,
    date_created timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_accepted timestamp NULL,
    date_denied timestamp NULL,
    date_cancelled timestamp NULL,
    user_id UUID NOT NULL,
    institution_id varchar(256) NOT NULL,
    agreement_id varchar(256) NOT NULL,
    agreement_expires timestamp NOT NUlL,
    max_history int NOT NULL,
    requisition_id varchar(256) NULL,
    status varchar(256) NOT NULL,
    error_code varchar(256) NULL,
    error_detail varchar(256) NULL
);

CREATE UNIQUE INDEX unq_user_institute ON ${flyway:defaultSchema}.userconsent (user_id, institution_id);

CREATE TABLE ${flyway:defaultSchema}.useraccount (
	id uuid NOT NULL CONSTRAINT userbankac_pkey PRIMARY KEY,
    date_created timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
	userconsent_id UUID NOT NULL,
	account_id varchar(256) NOT NULL,
	CONSTRAINT fk_userconsent FOREIGN KEY (userconsent_id) REFERENCES ${flyway:defaultSchema}.userconsent (id) ON DELETE CASCADE
);
