# Registration lifecycle

## V2

- Party revocation sets `revokedAt` to the current UTC date. It cannot be reversed. Repeated revocation keeps the first date. It does not change service or instance status.
- Service deactivation includes its instances. Service and instance reactivation affects only the selected resource and requires usable parents. Updates do not reactivate resources.
- Revocation and deactivation keep registrations, relationships, history and certificate revocation status. V2 has no physical deletion.
- Existing data stays readable and editable. A revoked party or inactive parent prevents new services, instances and `IntendedUse` snapshots, and snapshot replacement or revocation.
- New access and issuer certificates require a usable party and an active service and instance. Existing certificates stay readable and revocable through the Certificate Authority.
- Until inactive parties are removed, an existing party with `active=false` is not usable, even if `revokedAt` is null.

Status operations require `X-API-KEY`:

`PATCH /v2/relying-parties/{id}/revoke`

`PATCH /v2/{services,instances}/{id}/{deactivate,activate}`

| Status | Meaning |
| --- | --- |
| `204` | Status operation completed. |
| `404` | Resource does not exist. |
| `400` | Reactivation requires usable parents. |

Blocked writes and certificate issuance return `404` for a missing or unusable target or required parent.

### Instance data

`PATCH /v2/instances/{id}/entitlements` changes only roles. `PATCH /v2/instances/{id}/eaas` changes only EAA data. Each operation keeps the other list.

`PUT /v2/instances/{id}` changes roles and EAA data in the same request. `X-API-KEY` is necessary for these operations.

See [Certificate ownership](certificate-ownership.md) for certificate deletion rules.
