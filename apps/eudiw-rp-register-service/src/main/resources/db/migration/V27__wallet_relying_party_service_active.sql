ALTER TABLE `wallet_relying_party_service`
    ADD COLUMN `active` BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE `wallet_relying_party_service`
JOIN `wallet_relying_party`
    ON wallet_relying_party.id = wallet_relying_party_service.wallet_relying_party_id
SET wallet_relying_party_service.active = FALSE
WHERE wallet_relying_party.active = FALSE;

UPDATE `relying_party_instance`
JOIN `wallet_relying_party_service`
    ON wallet_relying_party_service.id = relying_party_instance.wallet_relying_party_service_id
SET relying_party_instance.active = FALSE
WHERE wallet_relying_party_service.active = FALSE;
