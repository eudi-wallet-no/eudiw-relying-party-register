# eudiw-ca-service

> [!NOTE]
> This application is part of the National Sandbox for Digital Wallet.
> See https://docs.digdir.no/docs/lommebok/lommebok_om.html for more information.

EUDIW Certificate Authority Service is an API for issuing RP access certificates.

The application's API can be used to:
* Download root and intermediate CA certificates and CRLs
* Issue RP access certificates signed by an intermediate CA for access certificates

## Requirements
- Java 25
- Maven
- Docker

> [!WARNING]
> Access to Digitaliseringsdirektoratet infrastructure is required to run the application.

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


## Secrets
Clone https://github.com/eudi-wallet-no/eudiw-developer-secrets and follow the instructions in the README.

## Running the application locally

The `dev` and `docker` profiles runs the application with the same configuration (certs, url).

The local hosts file should include:
```
127.0.0.1 ca-service
```

### Maven
The application can be started with Maven:
```
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Docker
The application can be started with Docker compose:
```
docker-compose up --build
```

The application will run on http://ca-service/:9220 with the `docker`profile.
