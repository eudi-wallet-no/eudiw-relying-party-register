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

Requires Docker Desktop, or Colima on macOS with `host.docker.internal` support.
Configure environment files and API keys as described in
[eudiw-developer-secrets](https://github.com/eudi-wallet-no/eudiw-developer-secrets).

```bash
# Build and start the stack
docker compose up --build -d

# Build and start the stack without rp-register-service
docker compose up --build --scale rp-register-service=0

# Stop one service (for example, while running it locally)
docker compose stop rp-register-service

# Follow logs for a service
docker compose logs -f rp-register-service
```

Services in `docker-compose.yaml`:
`rp-register-service`, `rp-register-admin`, `rp-register-selfservice`,
`rp-register-lookup-web`, `ca-service`, `credential-registry`,
`trustlist-service`, `rp-register-db`, `ca-db`, `redis`.

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
