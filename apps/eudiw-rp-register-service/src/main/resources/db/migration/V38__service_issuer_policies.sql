ALTER TABLE `wallet_relying_party_service`
    ADD COLUMN `issuer_policies` JSON NOT NULL DEFAULT ('[]');
