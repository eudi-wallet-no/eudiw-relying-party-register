# eudiw-relying-party-register

> [!NOTE]
> Del av National Sandbox for Digital Wallet.
> Se https://docs.digdir.no/docs/lommebok/lommebok_om.html for mer informasjon.

Monorepo for EUDI Wallet Relying Party Register-applikasjoner.

## Applikasjoner

| Applikasjon | Beskrivelse | README |
| --- | --- | --- |
| [eudiw-rp-register-admin](apps/eudiw-rp-register-admin) | Relying Party Register Admin + selvbetjening | [README](apps/eudiw-rp-register-admin/README.md) |
| [eudiw-rp-register-service](apps/eudiw-rp-register-service) | Relying Party Register Service | [README](apps/eudiw-rp-register-service/README.md) |
| [eudiw-rp-register-lookup-web](apps/eudiw-rp-register-lookup-web) | Relying Party Register oppslag (innsyn) | [README](apps/eudiw-rp-register-lookup-web/README.md) |
| [eudiw-credential-registry](apps/eudiw-credential-registry) | Credential Issuer Registry | [README](apps/eudiw-credential-registry/README.md) |

## Struktur

```
apps/       Applikasjoner (én mappe per app)
docs/       Delt dokumentasjon på tvers av applikasjoner
```

Hver app har sin egen README med detaljer om oppsett, kjøring og testing.
