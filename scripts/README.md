# Database dump from systest

Requires `oc`, `fzf` and `mariadb-dump` (recommended) or `mysqldump`.
Log in to systest with `oc login`.

```shell
bash scripts/mariadb-systest-dump.sh
```

Select an available MariaDB pod and enter the root password in the hidden prompt.
Supports RP register, CA, Issuer and Status List; targets are defined in
`database_config` at the top of the script. The dump is saved with restricted
file permissions in the gitignored `scripts/dump/` folder. The source database
is not modified.

## Local import

"Import to local Docker" is the preferred option and **deletes all data in the
target database**. Start the configured local database container and stop
connected apps outside Docker before importing. The configured application
container is stopped automatically if it is running, and started again on
success; other apps are handled manually. On error/abort the dump is kept, the
database may be partially imported and a container stopped by the script remains
stopped. "Save dump" does not require Docker.

## Tests

```shell
python3 scripts/tests/test_dump_mysql.py
bash -n scripts/mariadb-systest-dump.sh
```

The tests use simulated commands, not systest or Docker.

## Local stack configuration test

Verifies the root `docker-compose.yaml` and the app `dev` profiles (apps, ports,
host overrides, database DNS, deleted app Compose files). Requires Docker for
`docker compose config`.

```shell
python3 scripts/tests/test_local_stack.py
```