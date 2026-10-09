# Database-dump fra systest

Krever `oc`, `fzf` og `mariadb-dump` (anbefalt) eller `mysqldump`.
Sett opp [eudiw-developer-secrets](https://github.com/eudi-wallet-no/eudiw-developer-secrets)
og logg inn mot systest med `oc login`.

## Kjøring

Kjør fra repoets rot:

```shell
bash scripts/dump-mysql.sh
```

Velg applikasjon og MariaDB-pod, og oppgi databasens root-passord i den skjulte prompten.
Støtter RP Register Service, CA Service, Issuer Server og Status List.

Skriptet kontrollerer systest-server og innlogging før menyene åpnes.
Token lagres ikke i skriptet, og kildedatabasen endres ikke.

Dumpen lagres med begrensede filrettigheter i `scripts/dump/`, som er
gitignorert. Databasenavnet for andre applikasjoner enn RP-registeret hentes
fra developer-secrets. Env-filen leses som data, ikke som shellkode.

## Lokal import av RP-registeret

«Importer til lokal Docker» er førstevalg; «Lagre dump» beholder bare filen.
**Import sletter alle data i lokale `rp_register_db`. Stopp IntelliJ-appen først.**

Start [RP-registerets Docker-avhengigheter](../apps/eudiw-rp-register-service/README.md#intellij-with-dependencies-in-docker) før import.

Skriptet krever lokal Docker og riktig Compose-prosjekt og databasecontainer.
Databaseinnlogging kontrolleres før import. En kjørende applikasjonscontainer
stoppes og startes igjen etter vellykket import. En allerede stoppet container
forblir stoppet. Ved importfeil eller avbrudd beholdes dumpen, og tjenesten
startes ikke igjen automatisk; databasen kan være delvis importert.

Tabellenes tegnsett og kollasjon følger dumpen. Databasen gjenskapes med den
lokale serverens standardverdier, som kan være forskjellige fra systest.
Skriptet konverterer ikke tabellene og kopierer ikke kildedatabasens standarder.

## Tester

```shell
python3 scripts/tests/test_dump_mysql.py
bash -n scripts/dump-mysql.sh
```

Testene bruker simulerte eksterne kommandoer og midlertidige filer. De kobler
ikke til systest eller Docker og endrer ikke lokale databaser.
