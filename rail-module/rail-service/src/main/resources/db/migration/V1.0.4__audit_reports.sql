-- change acknowledged indicator from boolean to timestamp (showing when it was acknowledged)
ALTER TABLE ${flyway:defaultSchema}.audit_issue DROP COLUMN acknowledged;
ALTER TABLE ${flyway:defaultSchema}.audit_issue ADD COLUMN acknowledged_datetime timestamp NULL;
