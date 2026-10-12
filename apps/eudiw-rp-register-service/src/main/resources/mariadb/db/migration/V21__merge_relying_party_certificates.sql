SELECT CHARACTER_SET_NAME, COLLATION_NAME
INTO @instance_id_charset, @instance_id_collation
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'relying_party_instance' AND COLUMN_NAME = 'id';

SELECT CHARACTER_SET_NAME, COLLATION_NAME
INTO @entitlement_id_charset, @entitlement_id_collation
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'relying_party_entitlement' AND COLUMN_NAME = 'id';

SET @create_certificates = CONCAT(
    'CREATE TABLE `relying_party_certificates` (',
    '`id` VARCHAR(36) NOT NULL,',
    '`type` VARCHAR(16) NOT NULL,',
    '`relying_party_instance_id` VARCHAR(36) CHARACTER SET ', @instance_id_charset,
        ' COLLATE ', @instance_id_collation, ',',
    '`entitlement_id` VARCHAR(36) CHARACTER SET ', @entitlement_id_charset,
        ' COLLATE ', @entitlement_id_collation, ',',
    '`issuer` VARCHAR(255) NOT NULL,',
    '`ca_id` VARCHAR(255) NOT NULL,',
    '`certificate_pem` VARCHAR(8192) NOT NULL,',
    '`subject_dn` VARCHAR(255) NOT NULL,',
    '`serial_no` VARCHAR(64) NOT NULL,',
    '`valid_from_ms` BIGINT NOT NULL DEFAULT (0),',
    '`valid_until_ms` BIGINT NOT NULL DEFAULT (0),',
    '`revocation_status` INTEGER NOT NULL DEFAULT (-1),',
    'PRIMARY KEY (`id`),',
    'CONSTRAINT `uq_rp_certificate_type_serial` UNIQUE (`type`, `serial_no`),',
    'CONSTRAINT `fk_rp_certificate_instance` FOREIGN KEY (`relying_party_instance_id`) ',
        'REFERENCES `relying_party_instance` (`id`) ON DELETE CASCADE,',
    'CONSTRAINT `fk_rp_certificate_entitlement` FOREIGN KEY (`entitlement_id`) ',
        'REFERENCES `relying_party_entitlement` (`id`) ON DELETE CASCADE,',
    'CONSTRAINT `ck_rp_certificate_owner` CHECK (',
        '(`type` = ''ACCESS'' AND `relying_party_instance_id` IS NOT NULL AND `entitlement_id` IS NULL) ',
        'OR (`type` = ''ISSUER'' AND `relying_party_instance_id` IS NULL AND `entitlement_id` IS NOT NULL))',
    ')'
);
PREPARE create_certificates FROM @create_certificates;
EXECUTE create_certificates;
DEALLOCATE PREPARE create_certificates;

INSERT INTO `relying_party_certificates` (
    `id`, `type`, `relying_party_instance_id`, `entitlement_id`, `issuer`, `ca_id`,
    `certificate_pem`, `subject_dn`, `serial_no`, `valid_from_ms`, `valid_until_ms`, `revocation_status`
)
SELECT
    `id`, 'ACCESS', `relying_party_instance_id`, NULL, `issuer`, `ca_id`,
    `certificate_pem`, `subject_dn`, `serial_no`, `valid_from_ms`, `valid_until_ms`, `revocation_status`
FROM `access_certificate`
UNION ALL
SELECT
    `id`, 'ISSUER', NULL, `entitlement_id`, `issuer`, `ca_id`,
    `certificate_pem`, `subject_dn`, `serial_no`, `valid_from_ms`, `valid_until_ms`, `revocation_status`
FROM `issuer_certificate`;

DROP TABLE `access_certificate`;
DROP TABLE `issuer_certificate`;
