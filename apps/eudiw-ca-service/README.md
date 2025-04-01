# eudiw-ca-service
EUDIW Certificate Authority Service is an API for issuing RP access certificates.

The application's API can be used to:
* Download root and intermediate CA certificates and CRLs
* Issue RP access certificates signed by an intermediate CA for access certificates

## Requirements
- Java 24
- Maven
- Docker

## Running the application locally

Profiles in the [resources](/src/main/resources) folder:

| Profile | Description                                |
|---------|--------------------------------------------|
| dev     | Local development                          |
| docker  | Docker locally, run by docker-compose file |
| systest | Systest environment                        |
| test    | Test environment                           |

The hosts file should include:
```
127.0.0.1 eudiw-ca-service
```

The application can be started with Maven:
```
mvn spring-boot:run -Dspring-boot.run.profiles=<profile>
```

The application can be started with Docker compose:
```
docker-compose up --build
```

The application will run on http://eudiw-ca-service:9220 .