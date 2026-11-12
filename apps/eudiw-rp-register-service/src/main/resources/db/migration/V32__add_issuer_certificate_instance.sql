ALTER TABLE relying_party_certificates
    DROP CONSTRAINT ck_rp_certificate_owner,
    ADD CONSTRAINT ck_rp_certificate_owner CHECK (
        (type = 'ACCESS' AND relying_party_instance_id IS NOT NULL AND entitlement_id IS NULL)
        OR (type = 'ISSUER' AND entitlement_id IS NOT NULL)
    );

UPDATE relying_party_certificates certificate
JOIN relying_party_entitlement entitlement ON entitlement.id = certificate.entitlement_id
SET certificate.relying_party_instance_id = entitlement.relying_party_instance_id
WHERE certificate.type = 'ISSUER';

ALTER TABLE relying_party_certificates
    DROP CONSTRAINT ck_rp_certificate_owner,
    ADD CONSTRAINT ck_rp_certificate_owner CHECK (
        (type = 'ACCESS' AND relying_party_instance_id IS NOT NULL AND entitlement_id IS NULL)
        OR (type = 'ISSUER' AND relying_party_instance_id IS NOT NULL AND entitlement_id IS NOT NULL)
    );
