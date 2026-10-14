SELECT CHARACTER_SET_NAME, COLLATION_NAME
INTO @party_id_charset, @party_id_collation
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wallet_relying_party' AND COLUMN_NAME = 'id';

SET @create_legal_person = CONCAT(
    'CREATE TABLE `legal_person` (',
    '`id` VARCHAR(36) CHARACTER SET ', @party_id_charset, ' COLLATE ', @party_id_collation, ' NOT NULL,',
    '`wallet_relying_party_id` VARCHAR(36) CHARACTER SET ', @party_id_charset, ' COLLATE ', @party_id_collation, ' NOT NULL,',
    '`legal_name` JSON NOT NULL,',
    '`established_by_law` JSON NOT NULL,',
    'PRIMARY KEY (`id`),',
    'CONSTRAINT `uq_legal_person_wallet_relying_party` UNIQUE (`wallet_relying_party_id`),',
    'CONSTRAINT `fk_legal_person_wallet_relying_party` FOREIGN KEY (`wallet_relying_party_id`) ',
        'REFERENCES `wallet_relying_party` (`id`) ON DELETE CASCADE',
    ')'
);
PREPARE create_legal_person FROM @create_legal_person;
EXECUTE create_legal_person;
DEALLOCATE PREPARE create_legal_person;

-- Seed the legal name from the existing name; V23 renames that column to trade_name.
INSERT INTO `legal_person` (`id`, `wallet_relying_party_id`, `legal_name`, `established_by_law`)
SELECT `id`, `id`, JSON_ARRAY(`legal_name`), JSON_ARRAY() FROM `wallet_relying_party`;
