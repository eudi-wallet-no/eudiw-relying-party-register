SELECT CHARACTER_SET_NAME, COLLATION_NAME
INTO @party_id_charset, @party_id_collation
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wallet_relying_party' AND COLUMN_NAME = 'id';

SET @create_legal_person = CONCAT(
    'CREATE TABLE `legal_person` (',
    '`id` VARCHAR(36) CHARACTER SET ', @party_id_charset, ' COLLATE ', @party_id_collation, ' NOT NULL,',
    '`wallet_relying_party_id` VARCHAR(36) CHARACTER SET ', @party_id_charset, ' COLLATE ', @party_id_collation, ' NOT NULL,',
    'PRIMARY KEY (`id`),',
    'CONSTRAINT `uq_legal_person_wallet_relying_party` UNIQUE (`wallet_relying_party_id`),',
    'CONSTRAINT `fk_legal_person_wallet_relying_party` FOREIGN KEY (`wallet_relying_party_id`) ',
        'REFERENCES `wallet_relying_party` (`id`) ON DELETE CASCADE',
    ')'
);
PREPARE create_legal_person FROM @create_legal_person;
EXECUTE create_legal_person;
DEALLOCATE PREPARE create_legal_person;

SET @create_legal_name = CONCAT(
    'CREATE TABLE `legal_person_legal_name` (',
    '`legal_person_id` VARCHAR(36) CHARACTER SET ', @party_id_charset, ' COLLATE ', @party_id_collation, ' NOT NULL,',
    '`position` INTEGER NOT NULL,',
    '`legal_name` VARCHAR(255) NOT NULL,',
    'PRIMARY KEY (`legal_person_id`, `position`),',
    'CONSTRAINT `fk_legal_person_legal_name` FOREIGN KEY (`legal_person_id`) ',
        'REFERENCES `legal_person` (`id`) ON DELETE CASCADE',
    ')'
);
PREPARE create_legal_name FROM @create_legal_name;
EXECUTE create_legal_name;
DEALLOCATE PREPARE create_legal_name;

SET @create_laws = CONCAT(
    'CREATE TABLE `legal_person_established_by_law` (',
    '`legal_person_id` VARCHAR(36) CHARACTER SET ', @party_id_charset, ' COLLATE ', @party_id_collation, ' NOT NULL,',
    '`position` INTEGER NOT NULL,',
    '`legislative_identifier` VARCHAR(2048) NOT NULL,',
    '`legal_basis` VARCHAR(2048),',
    'PRIMARY KEY (`legal_person_id`, `position`),',
    'CONSTRAINT `fk_legal_person_established_by_law` FOREIGN KEY (`legal_person_id`) ',
        'REFERENCES `legal_person` (`id`) ON DELETE CASCADE',
    ')'
);
PREPARE create_laws FROM @create_laws;
EXECUTE create_laws;
DEALLOCATE PREPARE create_laws;

INSERT INTO `legal_person` (`id`, `wallet_relying_party_id`)
SELECT `id`, `id` FROM `wallet_relying_party`;

-- Seed the legal name from the existing name; V23 renames that column to trade_name.
INSERT INTO `legal_person_legal_name` (`legal_person_id`, `position`, `legal_name`)
SELECT `id`, 0, `legal_name` FROM `wallet_relying_party`;
