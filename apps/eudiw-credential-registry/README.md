# eudiw-credential-registry

> [!NOTE]
> This application is part of the National Sandbox for Digital Wallet.
> See https://docs.digdir.no/docs/lommebok/lommebok_om.html for more information.

EUDI wallet: credential-registry (bevisregister) for eidas2sandkasse.

## Requirements
- Java 25
- Maven
- Docker

> [!WARNING]
> Access to Digitaliseringsdirektoratet infrastructure is required to run the application.


## Development

### Secrets
Clone https://github.com/eudi-wallet-no/eudiw-developer-secrets and follow the instructions in the README.

### Profiles

Profiles in the [resources](/src/main/resources) folder:

| Profile | Description                                |
|---------|--------------------------------------------|
| dev     | Local development                          |
| docker  | Docker locally, run by docker-compose file |
| systest | Systest environment                        |
| test    | Test environment                           |
| prod    | Prod environment                           |


### Running the application locally

The local hosts file should include:
```
127.0.0.1 credential-registry
```

#### Docker
Run the Docker stack: `docker compose up --build`.

#### IntelliJ with dependencies in Docker
Run with profile `dev`; start dependencies with `docker compose up -d --build --scale credential-registry=0`.

The application will run on http://credential-registry:9294.
