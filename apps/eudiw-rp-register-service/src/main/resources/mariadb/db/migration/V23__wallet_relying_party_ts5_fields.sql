ALTER TABLE `wallet_relying_party`
    ADD COLUMN `country` VARCHAR(2) NOT NULL DEFAULT 'NO',
    ADD COLUMN `trade_name` VARCHAR(255),
    ADD COLUMN `provider_type` VARCHAR(64) NOT NULL DEFAULT 'WalletRelyingParty';

SELECT CHARACTER_SET_NAME, COLLATION_NAME
INTO @party_id_charset, @party_id_collation
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wallet_relying_party' AND COLUMN_NAME = 'id';

-- Additional identifiers only; orgno remains the national unique identifier in wallet_relying_party.orgno.
SET @create_identifiers = CONCAT(
    'CREATE TABLE `wallet_relying_party_identifiers` (',
    '`wallet_relying_party_id` VARCHAR(36) CHARACTER SET ', @party_id_charset, ' COLLATE ', @party_id_collation, ' NOT NULL,',
    '`position` INTEGER NOT NULL,',
    '`identifier_type` VARCHAR(2048) NOT NULL,',
    '`identifier_value` VARCHAR(255) NOT NULL,',
    'PRIMARY KEY (`wallet_relying_party_id`, `position`),',
    'CONSTRAINT `fk_wallet_relying_party_identifiers` FOREIGN KEY (`wallet_relying_party_id`) ',
        'REFERENCES `wallet_relying_party` (`id`) ON DELETE CASCADE',
    ')'
);
PREPARE create_identifiers FROM @create_identifiers;
EXECUTE create_identifiers;
DEALLOCATE PREPARE create_identifiers;

SET @create_policies = CONCAT(
    'CREATE TABLE `wallet_relying_party_policies` (',
    '`wallet_relying_party_id` VARCHAR(36) CHARACTER SET ', @party_id_charset, ' COLLATE ', @party_id_collation, ' NOT NULL,',
    '`position` INTEGER NOT NULL,',
    '`policy_type` VARCHAR(2048) NOT NULL,',
    '`policy_uri` VARCHAR(2048) NOT NULL,',
    'PRIMARY KEY (`wallet_relying_party_id`, `position`),',
    'CONSTRAINT `fk_wallet_relying_party_policies` FOREIGN KEY (`wallet_relying_party_id`) ',
        'REFERENCES `wallet_relying_party` (`id`) ON DELETE CASCADE',
    ')'
);
PREPARE create_policies FROM @create_policies;
EXECUTE create_policies;
DEALLOCATE PREPARE create_policies;

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

SET @create_postal_addresses = CONCAT(
    'CREATE TABLE `wallet_relying_party_postal_addresses` (',
    '`wallet_relying_party_id` VARCHAR(36) CHARACTER SET ', @party_id_charset, ' COLLATE ', @party_id_collation, ' NOT NULL,',
    '`position` INTEGER NOT NULL,',
    '`postal_address` VARCHAR(2048) NOT NULL,',
    'PRIMARY KEY (`wallet_relying_party_id`, `position`),',
    'CONSTRAINT `fk_wallet_relying_party_postal_addresses` FOREIGN KEY (`wallet_relying_party_id`) ',
        'REFERENCES `wallet_relying_party` (`id`) ON DELETE CASCADE',
    ')'
);
PREPARE create_postal_addresses FROM @create_postal_addresses;
EXECUTE create_postal_addresses;
DEALLOCATE PREPARE create_postal_addresses;

SET @create_emails = CONCAT(
    'CREATE TABLE `wallet_relying_party_emails` (',
    '`wallet_relying_party_id` VARCHAR(36) CHARACTER SET ', @party_id_charset, ' COLLATE ', @party_id_collation, ' NOT NULL,',
    '`position` INTEGER NOT NULL,',
    '`email` VARCHAR(320) NOT NULL,',
    'PRIMARY KEY (`wallet_relying_party_id`, `position`),',
    'CONSTRAINT `fk_wallet_relying_party_emails` FOREIGN KEY (`wallet_relying_party_id`) ',
        'REFERENCES `wallet_relying_party` (`id`) ON DELETE CASCADE',
    ')'
);
PREPARE create_emails FROM @create_emails;
EXECUTE create_emails;
DEALLOCATE PREPARE create_emails;

SET @create_phones = CONCAT(
    'CREATE TABLE `wallet_relying_party_phones` (',
    '`wallet_relying_party_id` VARCHAR(36) CHARACTER SET ', @party_id_charset, ' COLLATE ', @party_id_collation, ' NOT NULL,',
    '`position` INTEGER NOT NULL,',
    '`phone` VARCHAR(64) NOT NULL,',
    'PRIMARY KEY (`wallet_relying_party_id`, `position`),',
    'CONSTRAINT `fk_wallet_relying_party_phones` FOREIGN KEY (`wallet_relying_party_id`) ',
        'REFERENCES `wallet_relying_party` (`id`) ON DELETE CASCADE',
    ')'
);
PREPARE create_phones FROM @create_phones;
EXECUTE create_phones;
DEALLOCATE PREPARE create_phones;

SET @create_info_uris = CONCAT(
    'CREATE TABLE `wallet_relying_party_info_uris` (',
    '`wallet_relying_party_id` VARCHAR(36) CHARACTER SET ', @party_id_charset, ' COLLATE ', @party_id_collation, ' NOT NULL,',
    '`position` INTEGER NOT NULL,',
    '`info_uri` VARCHAR(2048) NOT NULL,',
    'PRIMARY KEY (`wallet_relying_party_id`, `position`),',
    'CONSTRAINT `fk_wallet_relying_party_info_uris` FOREIGN KEY (`wallet_relying_party_id`) ',
        'REFERENCES `wallet_relying_party` (`id`) ON DELETE CASCADE',
    ')'
);
PREPARE create_info_uris FROM @create_info_uris;
EXECUTE create_info_uris;
DEALLOCATE PREPARE create_info_uris;

SET @create_authority = CONCAT(
    'CREATE TABLE `supervisory_authority` (',
    '`id` VARCHAR(36) CHARACTER SET ', @party_id_charset, ' COLLATE ', @party_id_collation, ' NOT NULL,',
    '`wallet_relying_party_id` VARCHAR(36) CHARACTER SET ', @party_id_charset, ' COLLATE ', @party_id_collation, ' NOT NULL,',
    '`name` VARCHAR(255) NOT NULL,',
    '`country` VARCHAR(2) NOT NULL,',
    'PRIMARY KEY (`id`),',
    'CONSTRAINT `uq_supervisory_authority_wallet_relying_party` UNIQUE (`wallet_relying_party_id`),',
    'CONSTRAINT `fk_supervisory_authority_wallet_relying_party` FOREIGN KEY (`wallet_relying_party_id`) ',
        'REFERENCES `wallet_relying_party` (`id`) ON DELETE CASCADE',
    ')'
);
PREPARE create_authority FROM @create_authority;
EXECUTE create_authority;
DEALLOCATE PREPARE create_authority;

SET @create_authority_emails = CONCAT(
    'CREATE TABLE `supervisory_authority_emails` (',
    '`supervisory_authority_id` VARCHAR(36) CHARACTER SET ', @party_id_charset, ' COLLATE ', @party_id_collation, ' NOT NULL,',
    '`position` INTEGER NOT NULL,',
    '`email` VARCHAR(320) NOT NULL,',
    'PRIMARY KEY (`supervisory_authority_id`, `position`),',
    'CONSTRAINT `fk_supervisory_authority_emails` FOREIGN KEY (`supervisory_authority_id`) ',
        'REFERENCES `supervisory_authority` (`id`) ON DELETE CASCADE',
    ')'
);
PREPARE create_authority_emails FROM @create_authority_emails;
EXECUTE create_authority_emails;
DEALLOCATE PREPARE create_authority_emails;

SET @create_authority_phones = CONCAT(
    'CREATE TABLE `supervisory_authority_phones` (',
    '`supervisory_authority_id` VARCHAR(36) CHARACTER SET ', @party_id_charset, ' COLLATE ', @party_id_collation, ' NOT NULL,',
    '`position` INTEGER NOT NULL,',
    '`phone` VARCHAR(64) NOT NULL,',
    'PRIMARY KEY (`supervisory_authority_id`, `position`),',
    'CONSTRAINT `fk_supervisory_authority_phones` FOREIGN KEY (`supervisory_authority_id`) ',
        'REFERENCES `supervisory_authority` (`id`) ON DELETE CASCADE',
    ')'
);
PREPARE create_authority_phones FROM @create_authority_phones;
EXECUTE create_authority_phones;
DEALLOCATE PREPARE create_authority_phones;

SET @create_authority_form_uris = CONCAT(
    'CREATE TABLE `supervisory_authority_form_uris` (',
    '`supervisory_authority_id` VARCHAR(36) CHARACTER SET ', @party_id_charset, ' COLLATE ', @party_id_collation, ' NOT NULL,',
    '`position` INTEGER NOT NULL,',
    '`form_uri` VARCHAR(2048) NOT NULL,',
    'PRIMARY KEY (`supervisory_authority_id`, `position`),',
    'CONSTRAINT `fk_supervisory_authority_form_uris` FOREIGN KEY (`supervisory_authority_id`) ',
        'REFERENCES `supervisory_authority` (`id`) ON DELETE CASCADE',
    ')'
);
PREPARE create_authority_form_uris FROM @create_authority_form_uris;
EXECUTE create_authority_form_uris;
DEALLOCATE PREPARE create_authority_form_uris;

INSERT INTO `supervisory_authority` (`id`, `wallet_relying_party_id`, `name`, `country`)
SELECT `id`, `id`, 'Digitaliseringsdirektoratet', 'NO' FROM `wallet_relying_party`;
