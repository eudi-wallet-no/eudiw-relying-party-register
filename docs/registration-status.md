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

## Certificates in admin and self-service (EUW-2005)

The existing web registration ID identifies a V2 **Instance**, not a Party or
Service. Access and issuer certificate operations use:

- `POST /v2/instances/{instanceId}/certificates/{access|issuer}` to issue;
- `GET /v2/instances/{instanceId}/certificates/{access|issuer}/{certificateId}` to retrieve;
- `PATCH /v2/instances/{instanceId}/certificates/{access|issuer}/{certificateId}/revoke` to revoke explicitly.

The web controllers first retrieve the registration and enforce the user's
organisation access. Self-service is restricted to the user's organisation;
admins retain access across organisations. The internal API key does not replace
this check. Certificate retrieval and revocation are scoped to the owning
Instance in the register. The register checks the entire active hierarchy before
issuance, but inactivity does not prevent retrieval or explicit revocation.

### Preserved functionality and error outcomes

| Behaviour | Verification |
| --- | --- |
| Access and issuer issuance retain CSR forms, validation and issuer entitlement | `CsrControllerTests`; `V2CertificateClientTests` checks the Instance paths and request content; `RegistrationWorkflowTests` exercises the web-to-V2 transport without mocking the admin service |
| Both types retain organisation access and admin access for issuance, retrieval and revocation | CSR, download and revoke controller tests |
| Certificate retrieval retains the certificate and revocation status | `V2CertificateClientTests`; existing detail-view tests |
| Download retains PEM, `application/x-pem-file`, and the serial-number `.pem` attachment name, including revoked certificates | `DownloadCertificateControllerTests` |
| Missing certificates, rejected requests, CA failures and timeouts remain errors, not successful issuance or revocation | Client and controller tests; `CertificateRevocationTransactionTest` protects register status on failed CA calls |
| Inactive Party, Service or Instance prevents issuance; reads and explicit revocation remain available, and another Instance cannot retrieve or revoke the certificate | V2 registration-status HTTP integration tests |

Before revoking, the certificate client reads the owned certificate. If the
register already confirms a revoked status, the web operation completes without
another PATCH or CA call. Otherwise, only a `204` revocation response confirms
success. Issuance and retrieval require the API's `200` and a complete certificate
response; missing identity, certificate or revocation status is not success.

Certificate transport has no automatic retry, redirect following or V1 fallback.
A timeout or lost response leaves the issuance/revocation outcome unknown and
shows the existing error page; it does not claim that the remote operation failed
without effects. There is no background polling, automatic reconciliation or
user retry lock. A later explicit attempt checks register-confirmed status again.

**Known limitation:** CA revocation can succeed before the response is lost or
register persistence fails. The CA and register may then disagree. The same race
can occur between the preliminary read and a concurrent revocation. If only the
CA knows that the certificate is revoked, the current CA rejects another
revocation and the web shows an error. General CA/register reconciliation is not
implemented or assigned to EUW-2005; it requires a separate decision.

### Remaining V2 migration work

This is not a declaration that all admin/self-service functionality has migrated.
The remaining V1 calls on this branch are creation (`POST /v1/rp`), editing
(`PUT /v1/rp/{id}`), admin search (`POST /v1/rp/search`) and the role catalogue
(`GET /v1/entitlement`). They are intentionally preserved here. No lookup-web or
backend-V1 behaviour is removed.

The coordinator's checked task overview distinguishes implementation, merge and
board status:

| Reference | State and remaining responsibility |
| --- | --- |
| T1 / EUW-1990 [#119](https://github.com/eudi-wallet-no/eudiw-relying-party-register/pull/119), EUW-1992 [#118](https://github.com/eudi-wallet-no/eudiw-relying-party-register/pull/118) | List and admin sorting migrated and merged |
| T2 / EUW-1991 [#131](https://github.com/eudi-wallet-no/eudiw-relying-party-register/pull/131) | Shared registration lookup/details migrated and merged; individual certificate reads belong to T10 below |
| T4 / EUW-2003 [#133](https://github.com/eudi-wallet-no/eudiw-relying-party-register/pull/133) | Search, filters and role catalogue implemented and reported verified in a separate session; PR still open at this check. Its board status does not indicate missing implementation. Not a dependency of EUW-2005 |
| T5 / T6 / T10 / EUW-2005 | Issuance, explicit revocation and owned certificate download for both types are covered here; independent of EUW-2003 |
| T7, Jira not assigned | Migrate editing while preserving full writable state, ownership, separate status changes and certificate-bound role/issuer-URL restrictions. Actual Service description and at least one contact are required on save; reads tolerate missing data. Do not use Party PUT merely for Service fields |
| T8, Jira not assigned | Migrate creation through separate Party/Service/Instance operations; reuse Party by organisation number, use actual description/contact, `isIntermediary` and explicit Instance lists. Partial creation must not appear complete |
| T9, Jira not assigned | End documented admin/self-service V1-client use only after the remaining migration tasks. Do not remove or disable backend-V1 |
| T11 | Code-free mapping/decisions delivered; board completion has a process gap, not a new coding task. Formal dependency gap remains for T7/T8/T12/T13 |
| T12 / T13, Jira not assigned | Remaining selected display/form fields after consumer migration. Own organisation's manually maintained fields are editable, admins across organisations; external organisation data, ownership/server fields and role/status boundaries remain protected |

T7–T9 and T12–T13 remain in the coordinator's backlog. Newer decision notes take
precedence over outdated sequencing in card descriptions. Their implementation
and Jira assignment are not part of EUW-2005.

Separate, not yet assigned/decided tracks include lookup-web for the public
`/wrp` API, identifier/policy/subroles, conditional issuer/intermediary requirements,
the full IntendedUse lifecycle and possible Party.x5c management. These are not
additional acceptance criteria for this certificate migration. Neither these
tracks nor general CA/register reconciliation should be represented as solved.
