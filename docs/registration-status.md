# Deactivation and reactivation (V2)

Deactivation prevents new registrations and certificate issuance without
deleting the registration, its relationships or its history. This applies to
parties, services and instances.
V2 does not offer physical deletion.

Deactivating a party also deactivates its services and instances.
Deactivating a service deactivates its instances. Reactivation only affects the
selected resource, preventing descendants from being unintentionally brought
back into use. Reactivating a service requires an active party;
reactivating an instance requires both an active service and an active party.

Existing data can still be read and edited while the registration is inactive.
Updating a resource does not reactivate it. New services, instances and
`IntendedUse` snapshots cannot be created under inactive parents; existing
snapshots cannot be replaced or revoked there either.
These writes and certificate issuance return `404` when the target or a
required parent is missing or inactive.

Deactivation does not revoke certificates. Existing certificates and their
revocation status are preserved, and certificates can still be retrieved and
explicitly revoked through the Certificate Authority. Issuing new access or
issuer certificates requires the entire registration hierarchy to be active.

Change status with `PATCH /v2/{parties,services,instances}/{id}/deactivate` or
`/activate`. These operations require `X-API-KEY` and return `204` on success,
`404` for an unknown resource and `400` when attempting reactivation under
inactive parents. V1 retains physical deletion but cannot reactivate instances
under inactive parents.

## Certificate migration and remaining work

Admin and self-service certificate issuance, retrieval and explicit revocation
use V2, with the web registration ID identifying the owning Instance.

A timeout leaves the outcome unknown and is shown as an error, without automatic
retry or V1 fallback. If the register already confirms revocation, no new
revocation is sent. CA/register disagreement after a lost response or failed
persistence is not reconciled automatically; that requires a separate decision.

Creation and editing still use V1. Admin search and the role catalogue are
migrated separately in EUW-2003
([#133](https://github.com/eudi-wallet-no/eudiw-relying-party-register/pull/133)).
Remaining work covers creation, editing, selected display/form fields and finally
ending admin/self-service V1-client use. Backend-V1 and lookup-web are unchanged.
