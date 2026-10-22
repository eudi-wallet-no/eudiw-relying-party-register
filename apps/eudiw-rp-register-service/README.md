# eudiw-rp-register-service

> [!NOTE]
> This application is part of the National Sandbox for Digital Wallet.
> See https://docs.digdir.no/docs/lommebok/lommebok_om.html for more information.

EUDIW Relying Party Register

## Requirements
- Java 25
- Maven
- Docker

## WRP API signing configuration

The public `/wrp/{identifier}` endpoint signs its response using the configured
registry key. The corresponding public key is available from `GET /wrp/jwks`.
Set `WRP_ISSUER`, `WRP_REGISTRY_URI`, `WRP_JWKS_URL`, `WRP_SIGNING_KEY_ID`,
`WRP_SIGNING_KEYSTORE`, `WRP_SIGNING_KEYSTORE_TYPE`,
`WRP_SIGNING_KEYSTORE_PASSWORD`, `WRP_SIGNING_KEY_ALIAS`, and
`WRP_SIGNING_KEY_PASSWORD` in the service environment. Keep keystore material
and passwords in the external secrets configuration, not in the repository.

> [!WARNING]
> Access to Digitaliseringsdirektoratet infrastructure is required to run the application.

For local setup, run commands and ports, see
[Local development](../../README.md#run-everything-locally-with-docker-compose).
