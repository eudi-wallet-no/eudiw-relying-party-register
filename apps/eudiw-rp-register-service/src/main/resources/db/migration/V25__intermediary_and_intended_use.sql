ALTER TABLE `wallet_relying_party_service`
    ADD COLUMN `uses_intermediaries` JSON NOT NULL DEFAULT ('[]'),
    ADD COLUMN `is_intermediary` BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN `served_wrp_services` JSON NOT NULL DEFAULT ('[]');

SELECT CHARACTER_SET_NAME, COLLATION_NAME
INTO @service_id_charset, @service_id_collation
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'wallet_relying_party_service'
  AND COLUMN_NAME = 'id';

SET @create_intended_use = CONCAT(
    'CREATE TABLE `wallet_relying_party_service_intended_use` (',
    '`id` VARCHAR(36) CHARACTER SET ', @service_id_charset, ' COLLATE ', @service_id_collation, ' NOT NULL,',
    '`wallet_relying_party_service_id` VARCHAR(36) CHARACTER SET ', @service_id_charset, ' COLLATE ', @service_id_collation, ' NOT NULL,',
    '`intended_use_identifier` TEXT NULL,',
    '`created_at` VARCHAR(10) NULL,',
    '`revoked_at` VARCHAR(10) NULL,',
    '`purpose` JSON NULL,',
    '`privacy_policy` JSON NULL,',
    'PRIMARY KEY (`id`),',
    'CONSTRAINT `fk_wrp_service_intended_use_service` ',
        'FOREIGN KEY (`wallet_relying_party_service_id`) ',
        'REFERENCES `wallet_relying_party_service` (`id`) ON DELETE CASCADE',
    ')'
);
PREPARE create_intended_use FROM @create_intended_use;
EXECUTE create_intended_use;
DEALLOCATE PREPARE create_intended_use;
