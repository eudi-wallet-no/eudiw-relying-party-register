SELECT CHARACTER_SET_NAME, COLLATION_NAME
INTO @intended_use_id_charset, @intended_use_id_collation
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'wallet_relying_party_service_intended_use'
  AND COLUMN_NAME = 'id';

SET @create_credential = CONCAT(
    'CREATE TABLE `wallet_relying_party_service_intended_use_credential` (',
    '`id` VARCHAR(36) CHARACTER SET ', @intended_use_id_charset, ' COLLATE ', @intended_use_id_collation, ' NOT NULL,',
    '`intended_use_id` VARCHAR(36) CHARACTER SET ', @intended_use_id_charset, ' COLLATE ', @intended_use_id_collation, ' NOT NULL,',
    '`format` TEXT NOT NULL,',
    '`meta` JSON NOT NULL,',
    '`claims` JSON NOT NULL,',
    'PRIMARY KEY (`id`),',
    'CONSTRAINT `fk_wrp_intended_use_credential_intended_use` ',
        'FOREIGN KEY (`intended_use_id`) ',
        'REFERENCES `wallet_relying_party_service_intended_use` (`id`) ON DELETE CASCADE',
    ')'
);
PREPARE create_credential FROM @create_credential;
EXECUTE create_credential;
DEALLOCATE PREPARE create_credential;
