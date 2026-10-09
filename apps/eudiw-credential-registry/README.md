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


## Configuration

Profiles in the [resources](src/main/resources) folder:

| Profile | Description                                |
|---------|--------------------------------------------|
| dev     | Local development                          |
| docker  | Docker locally, run by the root docker-compose file |
| systest | Systest environment                        |
| test    | Test environment                           |
| prod    | Prod environment                           |

For local setup, run commands and ports, see
[Local development](../../README.md#running-locally-in-intellij).
