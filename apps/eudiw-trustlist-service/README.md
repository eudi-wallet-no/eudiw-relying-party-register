# eudiw-trustlist-service
EUDIW Trust List Service for eidas2sandkasse in Norway.

First version of trust lists is static and requires manual deploys to be updated.

Contains a list of EAA/Pub-EAA providers in one ETSI 119 612 trustlist that conforms to https://www.etsi.org/deliver/etsi_ts/119600_119699/119612/02.03.01_60/ts_119612v020301p.pdf.
ACA, PID and WALLET are separate ETSI 119 602 trustlists that confirms to https://www.etsi.org/deliver/etsi_ts/119600_119699/119602/01.01.01_60/ts_119602v010101p.pdf.

To add trusted enties/trusted-serviceproviders or services to the trusted listes, see manual update routine [here](UPDATE_ROUTINE.md).

## Requirements
- Java 25
- Maven
- Docker

## Configuration

Profiles in the [resources](/src/main/resources) folder:

| Profile | Description                                |
|---------|--------------------------------------------|
| dev     | Local development                          |
| docker  | Docker locally, run by docker-compose file |
| systest | Systest environment                        |
| test    | Test environment                           |


## Running the application locally

The `dev` and `docker` profiles runs the application with the same configuration (certs, url).

The local hosts file should include:
```
127.0.0.1 trustlist-service
```

The application can be started with Maven:
```
mvn spring-boot:run -Dspring-boot.run.profiles=<profile>
```

The application can be started with Docker compose:
```
docker-compose up --build
```

The application will run on http://trustlist-service:9230.
