# Database-dump fra systest

Krever `oc`, `fzf` og `mariadb-dump` (anbefalt) eller `mysqldump`.
Logg inn mot systest med `oc login`.

```shell
bash scripts/mariadb-systest-dump.sh
```

Velg en tilgjengelig MariaDB-pod og oppgi root-passordet i den skjulte prompten.
Støtter RP-register, CA, Issuer og Status List; mål defineres i `database_config`
øverst i skriptet. Dumpen lagres med begrensede filrettigheter i den
gitignorerte mappen `scripts/dump/`. Kildedatabasen endres ikke.

## Lokal import

«Importer til lokal Docker» er førstevalg og **sletter alle data i målbasen**.
Start den konfigurerte lokale databasecontaineren og stopp tilkoblede apper
utenfor Docker før import. Den konfigurerte applikasjonscontaineren stoppes
automatisk hvis den kjører, og startes igjen ved suksess; andre apper håndteres manuelt.
Ved feil/avbrudd beholdes dumpen, databasen kan være delvis importert og en
container stoppet av skriptet forblir stoppet. «Lagre dump» krever ikke Docker.

## Tester

```shell
python3 scripts/tests/test_dump_mysql.py
bash -n scripts/mariadb-systest-dump.sh
```

Testene bruker simulerte kommandoer, ikke systest eller Docker.
