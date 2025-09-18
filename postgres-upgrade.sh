# extract data from old database
docker compose -f postgres-upgrade.yaml up -d db-15
docker compose -f postgres-upgrade.yaml exec -it db-15 /bin/bash -c 'pg_dumpall -U $POSTGRES_USER > /backup/db-data.sql'
docker compose -f postgres-upgrade.yaml down

# copy data to new database
docker compose -f postgres-upgrade.yaml up -d db-17
docker compose -f postgres-upgrade.yaml exec -it db-17 /bin/bash -c 'psql -d $POSTGRES_DB -U $POSTGRES_USER < /backup/db-data.sql'
docker compose -f postgres-upgrade.yaml down

# rename volume containing upgraded db data
docker volume rm one-stop_db-data
docker volume create --name one-stop_db-data
docker run --rm -it -v one-stop_db-17-data:/from -v one-stop_db-data:/to alpine ash -c "cd /from ; cp -av . /to"
docker volume rm one-stop_db-17-data
