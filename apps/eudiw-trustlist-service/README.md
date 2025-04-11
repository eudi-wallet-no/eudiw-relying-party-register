# eudiw-trust-list-service
EUDIW Trust List Service

## Requirements
- Java 24
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
127.0.0.1 eudiw-trust-list-service
```

The application can be started with Maven:
```
mvn spring-boot:run -Dspring-boot.run.profiles=<profile>
```

The application can be started with Docker compose:
```
docker-compose up --build
```

The application will run on http://eudiw-trust-list-service:9230 .
