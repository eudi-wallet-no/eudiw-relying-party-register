SELECT CHARACTER_SET_NAME, COLLATION_NAME
INTO @service_id_charset, @service_id_collation
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'wallet_relying_party_service'
  AND COLUMN_NAME = 'id';

SELECT CHARACTER_SET_NAME, COLLATION_NAME
INTO @service_entitlement_id_charset, @service_entitlement_id_collation
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'wallet_relying_party_service_credential_issuer_url'
  AND COLUMN_NAME = 'id';

SET @create_service_entitlement = CONCAT(
    'CREATE TABLE wallet_relying_party_service_entitlement (',
    'id VARCHAR(36) CHARACTER SET ', @service_entitlement_id_charset,
        ' COLLATE ', @service_entitlement_id_collation, ' NOT NULL,',
    'wallet_relying_party_service_id VARCHAR(36) CHARACTER SET ', @service_id_charset,
        ' COLLATE ', @service_id_collation, ' NOT NULL,',
    'entitlement VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_nopad_bin NOT NULL,',
    'credential_issuer_url VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_nopad_bin,',
    'PRIMARY KEY (id),',
    'CONSTRAINT uq_service_entitlement UNIQUE (wallet_relying_party_service_id, entitlement),',
    'CONSTRAINT fk_service_entitlement_service FOREIGN KEY (wallet_relying_party_service_id) ',
        'REFERENCES wallet_relying_party_service (id) ON DELETE CASCADE',
    ')'
);
PREPARE create_service_entitlement FROM @create_service_entitlement;
EXECUTE create_service_entitlement;
DEALLOCATE PREPARE create_service_entitlement;
