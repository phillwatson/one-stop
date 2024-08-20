-- change acknowledged indicator from boolean to timestamp (showing when it was acknowledged)
ALTER TABLE rails.audit_issue DROP COLUMN acknowledged;
ALTER TABLE rails.audit_issue ADD COLUMN acknowledged_datetime timestamp NULL;
