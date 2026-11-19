SELECT CHARACTER_SET_NAME, COLLATION_NAME
INTO @service_id_charset, @service_id_collation
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'wallet_relying_party_service'
  AND COLUMN_NAME = 'id';

SET @create_provided_attestation = CONCAT(
    'CREATE TABLE wallet_relying_party_service_provided_attestation (',
    'id VARCHAR(36) CHARACTER SET ', @service_id_charset,
        ' COLLATE ', @service_id_collation, ' NOT NULL,',
    'wallet_relying_party_service_id VARCHAR(36) CHARACTER SET ', @service_id_charset,
        ' COLLATE ', @service_id_collation, ' NOT NULL,',
    'format TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_nopad_bin,',
    'type TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_nopad_bin,',
    'trusted_authorities JSON NOT NULL DEFAULT (''[]''),',
    'PRIMARY KEY (id),',
    'CONSTRAINT fk_service_provided_attestation_service ',
        'FOREIGN KEY (wallet_relying_party_service_id) ',
        'REFERENCES wallet_relying_party_service (id) ON DELETE CASCADE',
    ')'
);
PREPARE create_provided_attestation FROM @create_provided_attestation;
EXECUTE create_provided_attestation;
DEALLOCATE PREPARE create_provided_attestation;

INSERT INTO wallet_relying_party_service_provided_attestation
    (id, wallet_relying_party_service_id, format, type, trusted_authorities)
SELECT CAST(UUID() AS CHAR(36)),
       service.id,
       attestation.format,
       attestation.type,
       CASE
           WHEN JSON_TYPE(attestation.trusted_authorities) = 'ARRAY'
           THEN attestation.trusted_authorities
           ELSE JSON_ARRAY()
       END
FROM wallet_relying_party_service service
JOIN JSON_TABLE(
    service.provides_attestations,
    '$[*]' COLUMNS (
        format TEXT PATH '$.format' NULL ON EMPTY NULL ON ERROR,
        type TEXT PATH '$.type' NULL ON EMPTY NULL ON ERROR,
        trusted_authorities JSON PATH '$.trustedAuthorities' NULL ON EMPTY NULL ON ERROR
    )
) attestation ON TRUE;

ALTER TABLE wallet_relying_party_service
    DROP COLUMN provides_attestations;
