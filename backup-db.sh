docker run --rm \
--mount source=one-stop_db-data,target=/var/lib/postgresql/data \
-v $(pwd):/backup \
busybox \
tar -czvf /backup/one-stop_db-data.tar.gz /var/lib/postgresql/data
