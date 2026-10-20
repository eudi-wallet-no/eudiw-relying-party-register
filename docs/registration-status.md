# Registration status (V2)

V2 registrations can be deactivated without deleting their data. All operations
require the existing `X-API-KEY` header and return `204 No Content` on success.

| Resource | Deactivate (`PATCH`) | Reactivate (`PATCH`) |
| --- | --- | --- |
| Party | `/v2/parties/{id}/deactivate` | `/v2/parties/{id}/activate` |
| Service | `/v2/services/{id}/deactivate` | `/v2/services/{id}/activate` |
| Instance | `/v2/instances/{id}/deactivate` | `/v2/instances/{id}/activate` |

Deactivating a Party deactivates all its Services and instances. Deactivating a
Service deactivates all its instances. Reactivation only changes the selected
resource; its descendants remain inactive until explicitly reactivated.
Reactivating a child requires active ancestors (`400` otherwise). Unknown
resource IDs return `404`. V2 does not expose physical deletion.

Hierarchy writes acquire a database write lock on the owning Party before reading
registration state, including V1 creation, update and deletion. Writes within one
Party are serialized; unrelated Parties can still be changed concurrently.
These transactions use `READ_COMMITTED` so reads after waiting for the lock see
the preceding writer's committed state, not an earlier repeatable-read snapshot.

Certificates and their revocation status are preserved when registrations are
deactivated. New access and issuer certificate requests through the existing V1
API return `404` if the instance, its Service, or its Party is inactive. Certificate
retrieval and explicit revoke operations remain available for inactive
registrations. Revocation still uses the existing CA flow and only updates the
local status after the CA confirms it.

V1 request/response formats and physical deletion remain unchanged. Its instance
update operation cannot reactivate an instance under an inactive Party or
Service. The V27 migration adds the Service's `active` column with a default of
`TRUE`, and deactivates existing descendants of inactive Parties without changing
certificates.
