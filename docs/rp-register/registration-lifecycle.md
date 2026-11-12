# Registration lifecycle

## V2

- Deactivation keeps registrations, relationships, history and certificate revocation status. V2 has no physical deletion.
- Party deactivation includes its services and instances. Service deactivation includes its instances.
- Reactivation affects only the selected resource and requires active parents. Updates do not reactivate resources.
- Existing data stays readable and editable. Inactive parents prevent new services, instances and `IntendedUse` snapshots, and snapshot replacement or revocation.
- New access and issuer certificates require an active party, service and instance. Existing certificates stay readable and revocable through the Certificate Authority.

Status operations require `X-API-KEY`:

`PATCH /v2/{relying-parties,services,instances}/{id}/{deactivate,activate}`

| Status | Meaning |
| --- | --- |
| `204` | Status operation completed. |
| `404` | Resource does not exist. |
| `400` | Reactivation requires active parents. |

Blocked writes and certificate issuance return `404` for a missing or inactive target or required parent.

### Instance data

`PATCH /v2/instances/{id}/entitlements` changes only roles and issuer URLs. `PATCH /v2/instances/{id}/eaas` changes only EAA data. Each operation keeps the other list.

`PUT /v2/instances/{id}` changes roles, issuer URLs and EAA data in the same request. `X-API-KEY` is necessary for these operations.

## V1

V1 permits physical deletion but not instance reactivation under inactive parents.

See [Certificate ownership](certificate-ownership.md) for certificate deletion rules.
