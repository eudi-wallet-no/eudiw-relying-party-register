# eudiw-rp-register-service

> [!NOTE]
> This application is part of the National Sandbox for Digital Wallet.
> See https://docs.digdir.no/docs/lommebok/lommebok_om.html for more information.

EUDIW Relying Party Register

## Requirements
- Java 25
- Maven
- Docker

> [!WARNING]
> Access to Digitaliseringsdirektoratet infrastructure is required to run the application.

## Development

### Secrets
Clone https://github.com/eudi-wallet-no/eudiw-developer-secrets and follow the instructions in the README.

### Running the application locally

#### Docker
Run the Docker stack: `docker compose up --build`.

#### IntelliJ with dependencies in Docker
Run with profile `dev`; start dependencies with `docker compose up -d --build --scale rp-register-service=0`.
