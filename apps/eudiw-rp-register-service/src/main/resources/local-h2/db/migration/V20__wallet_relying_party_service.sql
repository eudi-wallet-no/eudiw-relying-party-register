ALTER TABLE `legal_entity` RENAME TO `wallet_relying_party`;
ALTER TABLE `wallet_relying_party` RENAME COLUMN `name` TO `legal_name`;
ALTER TABLE `wallet_relying_party` RENAME COLUMN `public_sector` TO `is_psb`;

CREATE TABLE `wallet_relying_party_service` (
    `id` VARCHAR(36) NOT NULL,
    `service_trade_name` VARCHAR(255) NOT NULL,
    `wallet_relying_party_id` VARCHAR(36) NOT NULL,
    `created_ms` BIGINT NOT NULL DEFAULT (0),
    `last_updated_ms` BIGINT NOT NULL DEFAULT (0),
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_wrp_service_wrp` FOREIGN KEY (`wallet_relying_party_id`)
        REFERENCES `wallet_relying_party` (`id`)
);

INSERT INTO `wallet_relying_party_service`
    (`id`, `service_trade_name`, `wallet_relying_party_id`, `created_ms`, `last_updated_ms`)
SELECT `id`, `trade_name`, `legal_entity_id`, `created_ms`, `last_updated_ms`
FROM `relying_party_instance`;

ALTER TABLE `relying_party_instance`
    ADD COLUMN `wallet_relying_party_service_id` VARCHAR(36);
UPDATE `relying_party_instance` SET `wallet_relying_party_service_id` = `id`;
ALTER TABLE `relying_party_instance`
    MODIFY COLUMN `wallet_relying_party_service_id` VARCHAR(36) NOT NULL;
ALTER TABLE `relying_party_instance`
    ADD CONSTRAINT `fk_rp_instance_wrp_service` FOREIGN KEY (`wallet_relying_party_service_id`)
        REFERENCES `wallet_relying_party_service` (`id`);
ALTER TABLE `relying_party_instance` DROP FOREIGN KEY `fk_rp_instance`;
ALTER TABLE `relying_party_instance` DROP COLUMN `legal_entity_id`;
ALTER TABLE `relying_party_instance` DROP COLUMN `trade_name`;
