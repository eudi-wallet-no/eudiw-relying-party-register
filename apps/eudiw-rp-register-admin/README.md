# eudiw-rp-register-admin

EUDIW Relying Party Register Admin + Self-service

## Requirements
- Java 25
- Maven
- Docker

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

The application can be started with Maven:
```
mvn spring-boot:run -Dspring-boot.run.profiles=<profile group>
```

The applications can be started with Docker compose:
```
docker compose up --build
```


## Ports

The admin and selfservice applications run on `http://rp-register-admin:9250`
and `http://rp-register-selfservice:9255`, respectively.
