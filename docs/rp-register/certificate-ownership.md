# Certificate ownership

## Issuer certificates

- Ownership checks use the certificate's instance reference. Role changes do not change the certificate's owner.
- The role is necessary for role information and Certificate Authority selection at issuance.
- Hibernate does not update the stored instance reference. This does not prevent an incorrect owner at certificate creation or changes with SQL.

## Deletion

You cannot delete a role with issuer certificates. This includes revoked certificates.

Before SQL deletion of an instance or service, delete the issuer certificates of the affected instances.

See [Registration lifecycle](registration-lifecycle.md) for deactivation and reactivation.
