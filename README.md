# eudiw-relying-party-register

> [!NOTE]
> Part of the National Sandbox for Digital Wallet.
> See https://docs.digdir.no/docs/lommebok/lommebok_om.html for more information.

Monorepo for EUDI Wallet Relying Party Register applications.

## Applications

| Application                                                       | Description                                 | README                                                |
|-------------------------------------------------------------------|---------------------------------------------|-------------------------------------------------------|
| [eudiw-rp-register-admin](apps/eudiw-rp-register-admin)           | Relying Party Register Admin + self-service | [README](apps/eudiw-rp-register-admin/README.md)      |
| [eudiw-rp-register-service](apps/eudiw-rp-register-service)       | Relying Party Register Service              | [README](apps/eudiw-rp-register-service/README.md)    |
| [eudiw-rp-register-lookup-web](apps/eudiw-rp-register-lookup-web) | Relying Party Register lookup (public view) | [README](apps/eudiw-rp-register-lookup-web/README.md) |
| [eudiw-credential-registry](apps/eudiw-credential-registry)       | Credential Issuer Registry                  | [README](apps/eudiw-credential-registry/README.md)    |
| [eudiw-trustlist-service](apps/eudiw-trustlist-service)           | Trust List Service for the eIDAS2 sandbox   | [README](apps/eudiw-trustlist-service/README.md)      |
| [eudiw-ca-service](apps/eudiw-ca-service)                         | CA Service for the eIDAS2 sandbox           | [README](apps/eudiw-ca-service/README.md)             |

## Structure

```
apps/       Applications (one folder per app)
docs/       Shared documentation across applications
```

Each app has its own README with application-specific setup and configuration.

## Test setup

Database tests start MariaDB automatically; Docker must be running.
If Testcontainers cannot find Docker when using Colima, set these variables
in `~/.zprofile` (not `~/.zshrc`):

```bash
export DOCKER_HOST="$(docker context inspect colima --format '{{.Endpoints.docker.Host}}')"
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
```

## Run everything locally with Docker Compose

Requires Docker with Docker Compose, for example Docker Desktop or Colima on macOS.
Configure environment files and API keys as described in
[eudiw-developer-secrets](https://github.com/eudi-wallet-no/eudiw-developer-secrets).

```bash
# Build and start the stack
docker compose up --build -d

# Follow logs for a Compose service
docker compose logs -f credential-registry
```

Services in `docker-compose.yaml`:
`rp-register-service`, `rp-register-admin`, `rp-register-selfservice`,
`rp-register-lookup-web`, `ca-service`, `credential-registry`,
`trustlist-service`, `rp-register-db`, `ca-db`, `redis`.

### Application, Ports

| Application             | API  | Management | Debug |
|-------------------------|------|------------|-------|
| rp-register-service     | 9200 | 9201       | 9205  |
| rp-register-lookup-web  | 9210 | 9211       | 9215  |
| rp-register-admin       | 9250 | 9250       | 9251  |
| rp-register-selfservice | 9255 | 9255       | 9256  |
| ca-service              | 9220 | 9221       | 9225  |
| credential-registry     | 9294 | 9294       | 9295  |
| trustlist-service       | 9230 | 9230       | 9235  |

To debug an app running in Docker, create an IntelliJ *Remote JVM Debug*
configuration with host `localhost` and the app's debug port. Rebuild the image
after code changes so breakpoints match the running code.
