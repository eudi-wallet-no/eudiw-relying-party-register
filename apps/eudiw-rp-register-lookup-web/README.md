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


## Configuration

Profiles in the [resources](src/main/resources) folder:

| Profile         | Description                                                        |
|-----------------|--------------------------------------------------------------------|
| `dev`           | Local development against local Docker dependencies (API on 9210, management on 9211) |
| `docker`        | Docker locally, run by the root docker-compose file                |
| `systest`       | Systest environment (deployed)                                     |
| `systest-local` | Run locally against the systest environment                        |
| `test`          | Test environment (deployed)                                        |

To run against the systest environment locally, run with profile `systest-local`.

For local setup, run commands and ports, see
[Local development](../../README.md#running-locally-in-intellij).
