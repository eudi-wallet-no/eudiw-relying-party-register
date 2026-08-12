DELETE FROM `relying_party_instance`
    WHERE `legal_entity_id` IS NULL;

ALTER TABLE `relying_party_instance`
    MODIFY COLUMN `legal_entity_id` VARCHAR(36) NOT NULL;
