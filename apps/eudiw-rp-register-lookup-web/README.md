# eudiw-rp-register-lookup-web

> [!NOTE]
> This application is part of the National Sandbox for Digital Wallet.
> See https://docs.digdir.no/docs/lommebok/lommebok_om.html for more information.

EUDIW Relying Party Register Web Interface

## Requirements
To build and run this project you need to have the following installed:
* Java 25
* Maven
* Docker

> [!WARNING]
> Access to Digitaliseringsdirektoratet infrastructure is required to run the application.


## Development

### Secrets
Clone https://github.com/eudi-wallet-no/eudiw-developer-secrets and follow the instructions in the README.

### Profiles

Profiles in the [resources](src/main/resources) folder:

| Profile         | Description                                                        |
|-----------------|--------------------------------------------------------------------|
| `dev`           | Local development against local Docker dependencies                |
| `docker`        | Docker locally, run by docker-compose file                         |
| `systest`       | Systest environment (deployed)                                     |
| `systest-local` | Run locally against the systest environment                        |
| `test`          | Test environment (deployed)                                        |

### Running the application locally

#### Docker
Run the Docker stack: `docker compose up --build`.

#### IntelliJ with dependencies in Docker
Run with profile `dev`; start dependencies with `docker compose up -d --build --scale rp-register-lookup-web=0`.

#### IntelliJ with dependencies in systest
Run with profile `systest-local`.
