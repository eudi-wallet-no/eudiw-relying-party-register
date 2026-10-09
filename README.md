# eudiw-relying-party-register

> [!NOTE]
> Part of the National Sandbox for Digital Wallet.
> See https://docs.digdir.no/docs/lommebok/lommebok_om.html for more information.

Monorepo for EUDI Wallet Relying Party Register applications.

## Applications

| Application | Description | README |
| --- | --- | --- |
| [eudiw-rp-register-admin](apps/eudiw-rp-register-admin) | Relying Party Register Admin + self-service | [README](apps/eudiw-rp-register-admin/README.md) |
| [eudiw-rp-register-service](apps/eudiw-rp-register-service) | Relying Party Register Service | [README](apps/eudiw-rp-register-service/README.md) |
| [eudiw-rp-register-lookup-web](apps/eudiw-rp-register-lookup-web) | Relying Party Register lookup (public view) | [README](apps/eudiw-rp-register-lookup-web/README.md) |
| [eudiw-credential-registry](apps/eudiw-credential-registry) | Credential Issuer Registry | [README](apps/eudiw-credential-registry/README.md) |
| [eudiw-trustlist-service](apps/eudiw-trustlist-service) | Trust List Service for the eIDAS2 sandbox | [README](apps/eudiw-trustlist-service/README.md) |
| [eudiw-ca-service](apps/eudiw-ca-service) | CA Service for the eIDAS2 sandbox | [README](apps/eudiw-ca-service/README.md) |

## Structure

```
apps/       Applications (one folder per app)
docs/       Shared documentation across applications
```

Each app has its own README with details on setup, running and testing.

## Run everything locally with Docker Compose

The root `docker-compose.yaml` is the only Compose file. It starts all seven
applications, two MariaDB databases, Redis and a dashboard with service links and readiness status.

### Secrets
Clone https://github.com/eudi-wallet-no/eudiw-developer-secrets and follow the instructions in the README. The Compose file reads the same environment files and API keys as the applications do when run from an IDE.



### Development

Start the whole stack with `docker compose up --build`. The dashboard is available at http://localhost:8080.

```bash
# Build and start all services in the background
docker compose up --build -d

# Build everything, but scale down one service because you run it locally
docker compose up --build --scale rp-register-service=0

# Stop all services (keep containers)
docker compose stop

# Show logs for one service, or all
docker compose logs -f rp-register-service
docker compose logs -f

# Stop and remove containers and network
docker compose down
```

### Running locally in IntelliJ

The `dev` profiles use the same ports, so an IntelliJ application and its Docker
container are interchangeable. Use `--scale <app>=0` in Docker, then run the
application in IntelliJ with `dev`.
For self-service, use `dev,selfservice-dev`.

### Application, Ports

| Application | API | Management |
| --- | --- | --- |
| rp-register-service | 9200 | 9201 |
| rp-register-lookup-web | 9210 | 9211 |
| rp-register-admin | 9250 | 9250 |
| rp-register-selfservice | 9255 | 9255 |
| ca-service | 9220 | 9221 |
| credential-registry | 9294 | 9294 |
| trustlist-service | 9230 | 9230 |
| dashboard | 8080 | — |
