ALTER TABLE `legal_entity` RENAME TO `wallet_relying_party`;
ALTER TABLE `wallet_relying_party` RENAME COLUMN `name` TO `legal_name`;
ALTER TABLE `wallet_relying_party` RENAME COLUMN `public_sector` TO `is_psb`;

-- Imported tables can have different collations from the database defaults.
-- Preserve the referenced ID collations on both sides of each foreign key.
SELECT CHARACTER_SET_NAME, COLLATION_NAME
INTO @party_id_charset, @party_id_collation
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wallet_relying_party' AND COLUMN_NAME = 'id';

SELECT CHARACTER_SET_NAME, COLLATION_NAME
INTO @instance_id_charset, @instance_id_collation
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'relying_party_instance' AND COLUMN_NAME = 'id';

SET @create_service = CONCAT(
    'CREATE TABLE `wallet_relying_party_service` (',
    '`id` VARCHAR(36) CHARACTER SET ', @instance_id_charset, ' COLLATE ', @instance_id_collation, ' NOT NULL,',
    '`service_trade_name` VARCHAR(255) NOT NULL,',
    '`wallet_relying_party_id` VARCHAR(36) CHARACTER SET ', @party_id_charset, ' COLLATE ', @party_id_collation, ' NOT NULL,',
    '`created_ms` BIGINT NOT NULL DEFAULT (0),',
    '`last_updated_ms` BIGINT NOT NULL DEFAULT (0),',
    'PRIMARY KEY (`id`),',
    'CONSTRAINT `fk_wrp_service_wrp` FOREIGN KEY (`wallet_relying_party_id`) REFERENCES `wallet_relying_party` (`id`)',
    ')'
);
PREPARE create_service FROM @create_service;
EXECUTE create_service;
DEALLOCATE PREPARE create_service;

INSERT INTO `wallet_relying_party_service`
    (`id`, `service_trade_name`, `wallet_relying_party_id`, `created_ms`, `last_updated_ms`)
SELECT `id`, `trade_name`, `legal_entity_id`, `created_ms`, `last_updated_ms`
FROM `relying_party_instance`;

SET @add_service_id = CONCAT(
    'ALTER TABLE `relying_party_instance` ADD COLUMN `wallet_relying_party_service_id` VARCHAR(36) CHARACTER SET ',
    @instance_id_charset, ' COLLATE ', @instance_id_collation
);
PREPARE add_service_id FROM @add_service_id;
EXECUTE add_service_id;
DEALLOCATE PREPARE add_service_id;

UPDATE `relying_party_instance` SET `wallet_relying_party_service_id` = `id`;
SET @require_service_id = CONCAT(
    'ALTER TABLE `relying_party_instance` MODIFY COLUMN `wallet_relying_party_service_id` VARCHAR(36) CHARACTER SET ',
    @instance_id_charset, ' COLLATE ', @instance_id_collation, ' NOT NULL'
);
PREPARE require_service_id FROM @require_service_id;
EXECUTE require_service_id;
DEALLOCATE PREPARE require_service_id;
ALTER TABLE `relying_party_instance`
    ADD CONSTRAINT `fk_rp_instance_wrp_service` FOREIGN KEY (`wallet_relying_party_service_id`)
        REFERENCES `wallet_relying_party_service` (`id`);
ALTER TABLE `relying_party_instance` DROP FOREIGN KEY `fk_rp_instance`;
ALTER TABLE `relying_party_instance` DROP COLUMN `legal_entity_id`;
ALTER TABLE `relying_party_instance` DROP COLUMN `trade_name`;
