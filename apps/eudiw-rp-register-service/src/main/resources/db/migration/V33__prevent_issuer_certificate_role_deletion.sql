ALTER TABLE relying_party_certificates
    DROP FOREIGN KEY IF EXISTS fk_rp_certificate_entitlement,
    ADD CONSTRAINT fk_rp_certificate_entitlement_restrict
        FOREIGN KEY IF NOT EXISTS (entitlement_id) REFERENCES relying_party_entitlement (id) ON DELETE RESTRICT;
