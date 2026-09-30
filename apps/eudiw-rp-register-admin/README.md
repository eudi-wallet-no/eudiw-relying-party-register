# eudiw-rp-register-admin

> [!NOTE]
> This application is part of the National Sandbox for Digital Wallet.
> See https://docs.digdir.no/docs/lommebok/lommebok_om.html for more information.

EUDIW Relying Party Register Admin + Self-service

## Requirements
- Java 25
- Maven
- Docker

> [!WARNING]
> Access to Digitaliseringsdirektoratet infrastructure is required to run the application.

## Applications, environments, and profiles

This project includes two applications: `rp-register-admin` and
`rp-register-selfservice`, and targets environments: dev, docker, systest, and
test. The combination of application/environment is controlled via Spring
profiles.

### Spring profiles

Profiles in the [resources](/src/main/resources) folder:

Environment specific profiles :

| Profile   | Description                                |
|-----------|--------------------------------------------|
| `dev`     | Local development                          |
| `docker`  | Docker locally, run by docker-compose file |
| `systest` | Systest environment                        |
| `test`    | Test environment                           |

Application specific profiles:

| Profile       | Description                                                     |
|---------------|-----------------------------------------------------------------|
| `admin`       | Properties used by rp-register-admin, regardless of environment |
| `selfservice` | Same as above, but for rp-register-selfservice                  |

Lastly, we have a number of profiles on the form `<application>-<env>`, which
apply specifically for a given combination of environment/application.

## Running the applications locally

The `dev,<application>,<application>-dev` and
`docker,<application>,<application>-docker` profile groups can be used to run
the applications with similar configuration.

The local hosts file should include:
```
127.0.0.1 rp-register-admin
127.0.0.1 rp-register-selfservice
```

### Maven

Start required dependencies with Docker Compose.
```
docker-compose up --scale rp-register-admin=0 -d
```

The application can be started with Maven:
```
mvn spring-boot:run -Dspring-boot.run.profiles=dev,admin-dev
```

### Docker
The applications can be started with Docker compose:
```
docker compose up --build
```


## Ports

The admin and selfservice applications run on `http://rp-register-admin:9250`
and `http://rp-register-selfservice:9255`, respectively.

## Content Security Policy

Both applications use `eudiw-admin-web.csp` from `application.yaml`. The
`policy` value is emitted by both security configurations. `mode: report-only`
sets `Content-Security-Policy-Report-Only`; switching to `mode: enforce` sets
`Content-Security-Policy` with the same policy. Unknown modes and missing
policy fail startup. Override the mode through the environment-specific Spring
configuration when ready; revert to `report-only` if legitimate browser
resources are blocked.

Before enforcing, verify that Digdir's report receiver accepts `report-uri`
reports at `https://csp-report.digdir.no/api/reports`, review reports for
potentially sensitive URLs, and check login/logout, registration/editing,
certificate revocation, search, error pages, and static assets in both
applications. Report-Only does not block resources or close the missing-CSP
finding. After enabling enforcement, check the public HTTPS response headers
and browser console; retain the existing HSTS, X-Content-Type-Options, and
X-Frame-Options headers.
