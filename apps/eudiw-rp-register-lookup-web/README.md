# eudiw-rp-register-lookup-web

> [!NOTE]
> This application is part of the National Sandbox for Digital Wallet.
> See https://docs.digdir.no/docs/lommebok/lommebok_om.html for more information.

EUDIW Relying Party Register Web Interface

## Requirements
To build and run this project you need to have the following installed:
* Java 25
* Maven

> [!WARNING]
> Access to Digitaliseringsdirektoratet infrastructure is required to run the application.

## Running the application locally
The application has several profiles located in the (resources)[src/main/resources] folder.

| Profile         | Description                                                        |
|-----------------|--------------------------------------------------------------------|
| `dev`           | Local development against local Docker dependencies                |
| `docker`        | Docker locally, run by docker-compose file                         |
| `systest`       | Systest environment (deployed)                                     |
| `systest-local` | Run locally against the systest environment                        |
| `test`          | Test environment (deployed)                                        |

### Running from commandline or IDE
The application can be started with Maven:

```
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```
