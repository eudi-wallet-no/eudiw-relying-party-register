ALTER TABLE `wallet_relying_party`
    ADD COLUMN `country` VARCHAR(2) NOT NULL DEFAULT 'NO',
    ADD COLUMN `provider_type` VARCHAR(64) NOT NULL DEFAULT 'WalletRelyingParty',
    ADD COLUMN `policy` JSON NULL,
    ADD COLUMN `identifier` JSON NULL,
    ADD COLUMN `postal_address` JSON NULL,
    ADD COLUMN `email` JSON NULL,
    ADD COLUMN `phone` JSON NULL,
    ADD COLUMN `info_uri` JSON NULL;

UPDATE `wallet_relying_party`
SET `policy` = COALESCE(`policy`, '[]'),
    `identifier` = COALESCE(`identifier`, '[]'),
    `postal_address` = COALESCE(`postal_address`, '[]'),
    `email` = COALESCE(`email`, '[]'),
    `phone` = COALESCE(`phone`, '[]'),
    `info_uri` = COALESCE(`info_uri`, '[]');
ALTER TABLE `wallet_relying_party`
    MODIFY COLUMN `policy` JSON NOT NULL,
    MODIFY COLUMN `identifier` JSON NOT NULL,
    MODIFY COLUMN `postal_address` JSON NOT NULL,
    MODIFY COLUMN `email` JSON NOT NULL,
    MODIFY COLUMN `phone` JSON NOT NULL,
    MODIFY COLUMN `info_uri` JSON NOT NULL;

-- legal_name has always held the trade name; legal names are stored in legal_person.legal_name (V22).
SELECT CHARACTER_SET_NAME, COLLATION_NAME
INTO @name_charset, @name_collation
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wallet_relying_party' AND COLUMN_NAME = 'legal_name';

SET @rename_legal_name = CONCAT(
    'ALTER TABLE `wallet_relying_party` CHANGE COLUMN `legal_name` `trade_name` VARCHAR(255) CHARACTER SET ',
    @name_charset, ' COLLATE ', @name_collation, ' NULL'
);
PREPARE rename_legal_name FROM @rename_legal_name;
EXECUTE rename_legal_name;
DEALLOCATE PREPARE rename_legal_name;

SELECT CHARACTER_SET_NAME, COLLATION_NAME
INTO @party_id_charset, @party_id_collation
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wallet_relying_party' AND COLUMN_NAME = 'id';

-- Additional identifiers only; orgno remains the national unique identifier in wallet_relying_party.orgno.
SET @create_x5c = CONCAT(
    'CREATE TABLE `wallet_relying_party_x5c` (',
    '`wallet_relying_party_id` VARCHAR(36) CHARACTER SET ', @party_id_charset, ' COLLATE ', @party_id_collation, ' NOT NULL,',
    '`position` INTEGER NOT NULL,',
    '`certificate_chain` TEXT NOT NULL,',
    'PRIMARY KEY (`wallet_relying_party_id`, `position`),',
    'CONSTRAINT `fk_wallet_relying_party_x5c` FOREIGN KEY (`wallet_relying_party_id`) ',
        'REFERENCES `wallet_relying_party` (`id`) ON DELETE CASCADE',
    ')'
);
PREPARE create_x5c FROM @create_x5c;
EXECUTE create_x5c;
DEALLOCATE PREPARE create_x5c;
