# eudiw-rp-register-service

> [!NOTE]
> This application is part of the National Sandbox for Digital Wallet.
> See https://docs.digdir.no/docs/lommebok/lommebok_om.html for more information.

EUDIW Relying Party Register

## API errors
Requests with an unsupported media type return HTTP 415 with error `invalid_request`
and description `HTTP media type not supported`, without echoing the supplied media type.

## Requirements
- Java 25
- Maven
- Docker

> [!WARNING]
> Access to Digitaliseringsdirektoratet infrastructure is required to run the application.

## Local development database
host: localhost
port: 36306
user: eudiw_user
password: lesssecret
database: register_service
