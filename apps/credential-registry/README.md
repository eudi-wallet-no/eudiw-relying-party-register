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

Profiles in the [resources](/src/main/resources) folder:

| Profile | Description                                |
|---------|--------------------------------------------|
| dev     | Local development                          |
| docker  | Docker locally, run by docker-compose file |
| systest | Systest environment                        |
| test    | Test environment                           |
| prod    | Prod environment                           |


## Running the application locally

The `dev` and `docker` profiles runs the application with similar configuration.

The local hosts file should include:
```
127.0.0.1 credential-registry
```

The application can be started with Maven:
```
mvn spring-boot:run -Dspring-boot.run.profiles=<profile>
```

The application can be started with Docker compose:
```
docker-compose up --build
```

The application will run on http://credential-registry:9294.
