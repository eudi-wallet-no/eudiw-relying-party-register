# eudiw-ca-service
EUDIW Certificate Authority Service is an API for issuing RP access certificates.

The application's API can be used to:
* Download root and intermediate CA certificates and CRLs
* Issue RP access certificates signed by an intermediate CA for access certificates

## Requirements
- Java 24
- Maven
- Docker

## Configuration

Tha application needs:
- a root CA certificate
- a set of intermediate CA certificates (issued by the root CA)
- to know the url it is running on (to add csr and cert info in issued certs)
- a database for storing issued leaf certificates

Profiles in the [resources](/src/main/resources) folder:

| Profile | Description                                          |
|---------|------------------------------------------------------|
| dev     | Local development w/embedded H2                      |
| docker  | Docker locally, run by docker-compose file w/MariaDB |
| systest | Systest environment                                  |
| test    | Test environment                                     |


## Running the application locally

The `dev` and `docker` profiles runs the application with the same configuration (certs, url).

The local hosts file should include:
```
127.0.0.1 ca-service
```

The application can be started with Maven:
```
mvn spring-boot:run -Dspring-boot.run.profiles=<profile>
```

The application can be started with Docker compose:
```
docker-compose up --build
```

The application will run on http://ca-service/:9220 .