# eudiw-rp-register-admin

> [!NOTE]
> This application is part of the National Sandbox for Digital Wallet.
> See https://docs.digdir.no/docs/lommebok/lommebok_om.html for more information.

EUDIW Relying Party Register Admin + Self-service

## Requirements
- Java 25
- Maven
- Docker

> [!WARNING]
> Access to Digitaliseringsdirektoratet infrastructure is required to run the application.


## Configuration

### Applications, environments, and profiles

This project includes two applications: `rp-register-admin` and
`rp-register-selfservice`, and targets environments: dev, docker, systest, and
test. The combination of application/environment is controlled via Spring
profiles.

### Spring profiles

Profiles in the [resources](src/main/resources) folder:

Environment specific profiles :

| Profile   | Description                                |
|-----------|--------------------------------------------|
| `dev`     | Local development                          |
| `docker`  | Docker locally, run by the root docker-compose file |
| `systest` | Systest environment                        |
| `test`    | Test environment                           |

Application specific profiles:

| Profile       | Description                                                     |
|---------------|-----------------------------------------------------------------|
| `admin`       | Properties used by rp-register-admin, regardless of environment |
| `selfservice` | Same as above, but for rp-register-selfservice                  |

Lastly, we have a number of profiles on the form `<application>-<env>`, which
apply specifically for a given combination of environment/application.

For local setup, run commands and ports, see
[Local development](../../README.md#running-locally-in-intellij).
