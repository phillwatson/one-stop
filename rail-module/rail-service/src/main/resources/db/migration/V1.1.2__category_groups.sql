-- create a table to hold the uniquely named category groups
CREATE TABLE rails.category_group (
    id uuid NOT NULL CONSTRAINT category_group_pkey PRIMARY KEY,
    user_id UUID NOT NULL,
    name varchar(256) NOT NULL,
    description varchar(256) NULL,
    version bigint NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX idx_category_group_name ON rails.category_group (user_id, name);

-- add a default group for each user - use user_id as the group id
INSERT INTO rails.category_group (id, version, name, description, user_id)
  select distinct c.user_id, 0, 'Default', 'Default group', c.user_id FROM rails.category c;

-- add a group_id column to the category table
ALTER TABLE rails.category ADD COLUMN group_id UUID NULL;

-- update the category table to link to the default group
UPDATE rails.category SET group_id = g.id
  FROM rails.category c
  JOIN rails.category_group g ON g.user_id = c.user_id;

-- make the group_id column not nullable
ALTER TABLE rails.category ALTER COLUMN group_id SET NOT NULL;

-- add a foreign key constraint to the category table
ALTER TABLE rails.category
  ADD CONSTRAINT fk_category_group
  FOREIGN KEY (group_id)
  REFERENCES rails.category_group (id) ON DELETE CASCADE;

-- make category names unique within its group rather than the user
DROP INDEX rails.idx_category_user_id CASCADE;
ALTER TABLE rails.category DROP COLUMN user_id;
CREATE UNIQUE INDEX idx_category_group_id ON rails.category (group_id, name);
