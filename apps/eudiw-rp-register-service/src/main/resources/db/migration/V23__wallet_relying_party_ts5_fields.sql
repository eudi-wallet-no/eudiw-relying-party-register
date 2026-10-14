ALTER TABLE `wallet_relying_party`
    ADD COLUMN `country` VARCHAR(2) NOT NULL DEFAULT 'NO',
    ADD COLUMN `provider_type` VARCHAR(64) NOT NULL DEFAULT 'WalletRelyingParty',
    ADD COLUMN `policy` JSON NULL;

UPDATE `wallet_relying_party` SET `policy` = '{}' WHERE `policy` IS NULL;
ALTER TABLE `wallet_relying_party` MODIFY COLUMN `policy` JSON NOT NULL;

-- legal_name has always held the trade name; the legal name now lives in legal_person_legal_name (V22).
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
SET @create_identifier = CONCAT(
    'CREATE TABLE `wallet_relying_party_identifier` (',
    '`wallet_relying_party_id` VARCHAR(36) CHARACTER SET ', @party_id_charset, ' COLLATE ', @party_id_collation, ' NOT NULL,',
    '`position` INTEGER NOT NULL,',
    '`identifier_type` VARCHAR(2048) NOT NULL,',
    '`identifier_value` VARCHAR(255) NOT NULL,',
    'PRIMARY KEY (`wallet_relying_party_id`, `position`),',
    'CONSTRAINT `fk_wallet_relying_party_identifier` FOREIGN KEY (`wallet_relying_party_id`) ',
        'REFERENCES `wallet_relying_party` (`id`) ON DELETE CASCADE',
    ')'
);
PREPARE create_identifier FROM @create_identifier;
EXECUTE create_identifier;
DEALLOCATE PREPARE create_identifier;

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

SET @create_postal_address = CONCAT(
    'CREATE TABLE `wallet_relying_party_postal_address` (',
    '`wallet_relying_party_id` VARCHAR(36) CHARACTER SET ', @party_id_charset, ' COLLATE ', @party_id_collation, ' NOT NULL,',
    '`position` INTEGER NOT NULL,',
    '`postal_address` VARCHAR(2048) NOT NULL,',
    'PRIMARY KEY (`wallet_relying_party_id`, `position`),',
    'CONSTRAINT `fk_wallet_relying_party_postal_address` FOREIGN KEY (`wallet_relying_party_id`) ',
        'REFERENCES `wallet_relying_party` (`id`) ON DELETE CASCADE',
    ')'
);
PREPARE create_postal_address FROM @create_postal_address;
EXECUTE create_postal_address;
DEALLOCATE PREPARE create_postal_address;

SET @create_email = CONCAT(
    'CREATE TABLE `wallet_relying_party_email` (',
    '`wallet_relying_party_id` VARCHAR(36) CHARACTER SET ', @party_id_charset, ' COLLATE ', @party_id_collation, ' NOT NULL,',
    '`position` INTEGER NOT NULL,',
    '`email` VARCHAR(320) NOT NULL,',
    'PRIMARY KEY (`wallet_relying_party_id`, `position`),',
    'CONSTRAINT `fk_wallet_relying_party_email` FOREIGN KEY (`wallet_relying_party_id`) ',
        'REFERENCES `wallet_relying_party` (`id`) ON DELETE CASCADE',
    ')'
);
PREPARE create_email FROM @create_email;
EXECUTE create_email;
DEALLOCATE PREPARE create_email;

SET @create_phone = CONCAT(
    'CREATE TABLE `wallet_relying_party_phone` (',
    '`wallet_relying_party_id` VARCHAR(36) CHARACTER SET ', @party_id_charset, ' COLLATE ', @party_id_collation, ' NOT NULL,',
    '`position` INTEGER NOT NULL,',
    '`phone` VARCHAR(64) NOT NULL,',
    'PRIMARY KEY (`wallet_relying_party_id`, `position`),',
    'CONSTRAINT `fk_wallet_relying_party_phone` FOREIGN KEY (`wallet_relying_party_id`) ',
        'REFERENCES `wallet_relying_party` (`id`) ON DELETE CASCADE',
    ')'
);
PREPARE create_phone FROM @create_phone;
EXECUTE create_phone;
DEALLOCATE PREPARE create_phone;

SET @create_info_uri = CONCAT(
    'CREATE TABLE `wallet_relying_party_info_uri` (',
    '`wallet_relying_party_id` VARCHAR(36) CHARACTER SET ', @party_id_charset, ' COLLATE ', @party_id_collation, ' NOT NULL,',
    '`position` INTEGER NOT NULL,',
    '`info_uri` VARCHAR(2048) NOT NULL,',
    'PRIMARY KEY (`wallet_relying_party_id`, `position`),',
    'CONSTRAINT `fk_wallet_relying_party_info_uri` FOREIGN KEY (`wallet_relying_party_id`) ',
        'REFERENCES `wallet_relying_party` (`id`) ON DELETE CASCADE',
    ')'
);
PREPARE create_info_uri FROM @create_info_uri;
EXECUTE create_info_uri;
DEALLOCATE PREPARE create_info_uri;
