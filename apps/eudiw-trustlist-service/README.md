# eudiw-trustlist-service

> [!NOTE]
> This application is part of the National Sandbox for Digital Wallet.
> See https://docs.digdir.no/docs/lommebok/lommebok_om.html for more information.

EUDIW Trust List Service for eidas2sandkasse in Norway.

First version of trust lists is static and requires manual deploys to be updated.

Contains a list of EAA/Pub-EAA providers in one ETSI 119 612 trustlist that conforms to https://www.etsi.org/deliver/etsi_ts/119600_119699/119612/02.03.01_60/ts_119612v020301p.pdf.
ACA, PID and WALLET are separate ETSI 119 602 trustlists that confirms to https://www.etsi.org/deliver/etsi_ts/119600_119699/119602/01.01.01_60/ts_119602v010101p.pdf.

To add trusted enties/trusted-serviceproviders or services to the trusted listes, see manual update routine [here](UPDATE_ROUTINE.md).

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

For local setup, run commands and ports, see
[Local development](../../README.md#running-locally-in-intellij).
