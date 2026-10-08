# eudiw-rp-register-service

> [!NOTE]
> This application is part of the National Sandbox for Digital Wallet.
> See https://docs.digdir.no/docs/lommebok/lommebok_om.html for more information.

EUDIW Relying Party Register

## Data model
The TS5 model stores a wallet relying party's legal name and PSB status in
`wallet_relying_party`. Each relying party instance has its own
`wallet_relying_party_service`, which holds the service trade name. Entitlements,
EAAs and certificates remain attached to the instance.

V20 migrates existing instances without changing their IDs or timestamps. New
registrations create a service and instance together; deleting a registration
removes both, but preserves the wallet relying party. The `/v1` JSON fields and
sorting aliases remain unchanged.

Run `mvn -q -f apps/eudiw-rp-register-service/pom.xml verify` from the repository
root. The JSON contract test normalizes only generated IDs and timestamps.
Migration tests cover H2 in MySQL mode and MariaDB 11.4 using Testcontainers;
the MariaDB test is skipped when Docker is unavailable.

With Colima, expose its Docker socket to Testcontainers before running Maven:
```sh
export DOCKER_HOST="$(docker context inspect --format '{{.Endpoints.docker.Host}}')"
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
```

## API errors
Requests with an unsupported media type return HTTP 415 with error `invalid_request`
and description `HTTP media type not supported`, without echoing the supplied media type.

## Requirements
- Java 25
- Maven
- Docker

> [!WARNING]
> Access to Digitaliseringsdirektoratet infrastructure is required to run the application.

## Local development database
host: localhost
port: 36306
user: eudiw_user
password: lesssecret
database: register_service
