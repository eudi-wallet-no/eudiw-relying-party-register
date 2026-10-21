# Registration status (V2)

`WalletRelyingParty`, `WalletRelyingPartyService` and `RelyingPartyInstance`
resources can be deactivated without deleting their data. All operations
require the existing `X-API-KEY` header and return `204 No Content` on success.

| Domain resource | Deactivate (`PATCH`) | Reactivate (`PATCH`) |
| --- | --- | --- |
| `WalletRelyingParty` | `/v2/parties/{id}/deactivate` | `/v2/parties/{id}/activate` |
| `WalletRelyingPartyService` | `/v2/services/{id}/deactivate` | `/v2/services/{id}/activate` |
| `RelyingPartyInstance` | `/v2/instances/{id}/deactivate` | `/v2/instances/{id}/activate` |

Deactivating a `WalletRelyingParty` deactivates all associated
`WalletRelyingPartyService` and `RelyingPartyInstance` resources. Deactivating a
`WalletRelyingPartyService` deactivates all associated `RelyingPartyInstance` resources.
Reactivation only changes the selected resource; its descendants remain inactive
until explicitly reactivated. Reactivating a child requires active ancestors
(`400` otherwise). Unknown resource identifiers return `404`. V2 does not expose
physical deletion.

Hierarchy writes acquire a database write lock on the owning
`WalletRelyingParty` before reading registration state, including V1 creation,
update and deletion. Writes within one `WalletRelyingParty` are serialized;
unrelated `WalletRelyingParty` resources can still be changed concurrently.
These transactions use `READ_COMMITTED` so reads after waiting for the lock see
the preceding writer's committed state, not an earlier repeatable-read snapshot.

Certificates and their revocation status are preserved when a resource is
deactivated. New access and issuer certificate requests through the existing V1
API return `404` if the `RelyingPartyInstance`, its
`WalletRelyingPartyService`, or its `WalletRelyingParty` is inactive. Certificate
retrieval and explicit revocation operations remain available for inactive
registrations. Revocation still uses the existing Certificate Authority flow
and only updates the local status after the Certificate Authority confirms it.
Issuance takes the same `WalletRelyingParty` lock as hierarchy writes before checking
registration state or contacting the Certificate Authority.

V1 request/response formats and physical deletion remain unchanged. The V1
`RelyingPartyInstance` update operation cannot reactivate a
`RelyingPartyInstance` under an
inactive `WalletRelyingParty` or `WalletRelyingPartyService`. The V27 migration
adds the `WalletRelyingPartyService` `active` column with a default of `TRUE`,
and deactivates existing descendants of inactive `WalletRelyingParty` resources
without changing certificates.
