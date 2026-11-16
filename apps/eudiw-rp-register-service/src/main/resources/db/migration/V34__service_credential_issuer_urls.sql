SELECT CHARACTER_SET_NAME, COLLATION_NAME
INTO @service_id_charset, @service_id_collation
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'wallet_relying_party_service'
  AND COLUMN_NAME = 'id';

SET @create_service_credential_issuer_url = CONCAT(
    'CREATE TABLE wallet_relying_party_service_credential_issuer_url (',
    'id VARCHAR(36) NOT NULL,',
    'wallet_relying_party_service_id VARCHAR(36) CHARACTER SET ', @service_id_charset,
        ' COLLATE ', @service_id_collation, ' NOT NULL,',
    'entitlement VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_nopad_bin NOT NULL,',
    'credential_issuer_url VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_nopad_bin,',
    'PRIMARY KEY (id),',
    'CONSTRAINT uq_service_credential_issuer_role UNIQUE (wallet_relying_party_service_id, entitlement),',
    'CONSTRAINT fk_service_credential_issuer_url FOREIGN KEY (wallet_relying_party_service_id) ',
        'REFERENCES wallet_relying_party_service (id) ON DELETE CASCADE',
    ')'
);
PREPARE create_service_credential_issuer_url FROM @create_service_credential_issuer_url;
EXECUTE create_service_credential_issuer_url;
DEALLOCATE PREPARE create_service_credential_issuer_url;

INSERT INTO wallet_relying_party_service_credential_issuer_url
    (id, wallet_relying_party_service_id, entitlement, credential_issuer_url)
SELECT UUID(), history.wallet_relying_party_service_id,
       history.entitlement, history.credential_issuer_url
FROM (
    SELECT DISTINCT instance.wallet_relying_party_service_id,
           CONVERT(entitlement.entitlement USING utf8mb4) COLLATE utf8mb4_nopad_bin AS entitlement,
           CONVERT(entitlement.credential_issuer_url USING utf8mb4)
               COLLATE utf8mb4_nopad_bin AS credential_issuer_url
    FROM relying_party_entitlement entitlement
    JOIN relying_party_instance instance ON instance.id = entitlement.relying_party_instance_id
    WHERE entitlement.credential_issuer_url IS NOT NULL
) history;

ALTER TABLE relying_party_entitlement DROP COLUMN credential_issuer_url;
