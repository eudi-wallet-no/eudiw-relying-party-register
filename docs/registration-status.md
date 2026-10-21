# Deactivation and reactivation (V2)

Deactivation prevents new registrations and certificate issuance without
deleting the registration, its relationships or its history. This applies to
`WalletRelyingParty`, `WalletRelyingPartyService` and `RelyingPartyInstance`.
V2 does not offer physical deletion.

Deactivating a `WalletRelyingParty` also deactivates its services and instances.
Deactivating a service deactivates its instances. Reactivation only affects the
selected resource, preventing descendants from being unintentionally brought
back into use. Reactivating a service requires an active `WalletRelyingParty`;
reactivating an instance requires both an active service and an active
`WalletRelyingParty`.

Existing data can still be read and edited while the registration is inactive.
Updating a resource does not reactivate it. New services, instances and
`IntendedUse` snapshots cannot be created under inactive parents; existing
snapshots cannot be replaced or revoked there either.

Deactivation does not revoke certificates. Existing certificates and their
revocation status are preserved, and certificates can still be retrieved and
explicitly revoked through the Certificate Authority. Issuing new access or
issuer certificates requires the entire registration hierarchy to be active.

Change status with `PATCH /v2/{parties,services,instances}/{id}/deactivate` or
`/activate`. These operations require `X-API-KEY` and return `204` on success,
`404` for an unknown resource and `400` when attempting reactivation under
inactive parents. V1 retains physical deletion but cannot reactivate instances
under inactive parents.
