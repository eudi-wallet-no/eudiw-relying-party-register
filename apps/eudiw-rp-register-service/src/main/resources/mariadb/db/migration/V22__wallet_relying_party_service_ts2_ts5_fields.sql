ALTER TABLE `wallet_relying_party_service`
    ADD COLUMN `service_identifier` TEXT,
    ADD COLUMN `support_uri` TEXT,
    ADD COLUMN `email` TEXT,
    ADD COLUMN `phone` TEXT,
    ADD COLUMN `srv_description` JSON,
    ADD COLUMN `sub_entitlements` JSON NOT NULL DEFAULT ('[]'),
    ADD COLUMN `provides_attestations` JSON NOT NULL DEFAULT ('[]');
