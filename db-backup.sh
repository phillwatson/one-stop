docker compose -f db-backup.yaml up -d db
docker compose -f db-backup.yaml exec -it db /bin/bash -c 'pg_dumpall -U $POSTGRES_USER > /backup/db-data-$(date +"%Y-%m-%d").sql'
docker compose -f db-backup.yaml down
