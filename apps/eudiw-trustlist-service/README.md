# eudiw-trustlist-service
EUDIW Trust List Service for eidas2sandkasse in Norway.

First version of trust list is static and requires manual deploys to be updated.

Conforms to https://www.etsi.org/deliver/etsi_ts/119600_119699/119612/02.03.01_60/ts_119612v020301p.pdf, but will be changed later when new specifications are ready and published from EU.

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
