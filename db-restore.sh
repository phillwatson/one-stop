# copy data to new database
docker compose -f db-backup.yaml up -d db
docker compose -f db-backup.yaml exec -it db /bin/bash -c 'psql -d $POSTGRES_DB -U $POSTGRES_USER < /backup/db-data.sql'
docker compose -f db-backup.yaml down
