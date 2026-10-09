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


## Development

### Secrets
Clone https://github.com/eudi-wallet-no/eudiw-developer-secrets and follow the instructions in the README.

### Profiles

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

### Running the application locally

The local hosts file should include:
```
127.0.0.1 ca-service
```

#### Docker
Run the Docker stack: `docker compose up --build`.

#### IntelliJ
Run with profile `dev` (embedded H2; no Docker dependencies).

The application will run on http://ca-service:9220 with either profile.
