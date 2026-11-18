SELECT CHARACTER_SET_NAME, COLLATION_NAME
INTO @service_entitlement_id_charset, @service_entitlement_id_collation
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'wallet_relying_party_service_entitlement'
  AND COLUMN_NAME = 'id';

INSERT INTO wallet_relying_party_service_entitlement
    (id, wallet_relying_party_service_id, entitlement, credential_issuer_url)
SELECT COALESCE(configuration.id, CAST(UUID() AS CHAR(36))), selections.wallet_relying_party_service_id,
       selections.entitlement, configuration.credential_issuer_url
FROM (
    SELECT DISTINCT instance.wallet_relying_party_service_id,
           CONVERT(entitlement.entitlement USING utf8mb4) COLLATE utf8mb4_nopad_bin AS entitlement
    FROM relying_party_entitlement entitlement
    JOIN relying_party_instance instance ON instance.id = entitlement.relying_party_instance_id
) selections
LEFT JOIN wallet_relying_party_service_credential_issuer_url configuration
    ON configuration.wallet_relying_party_service_id = selections.wallet_relying_party_service_id
   AND configuration.entitlement = selections.entitlement;

SET @add_service_entitlement_id = CONCAT(
    'ALTER TABLE relying_party_entitlement ADD COLUMN service_entitlement_id VARCHAR(36) CHARACTER SET ',
    @service_entitlement_id_charset, ' COLLATE ', @service_entitlement_id_collation
);
PREPARE add_service_entitlement_id FROM @add_service_entitlement_id;
EXECUTE add_service_entitlement_id;
DEALLOCATE PREPARE add_service_entitlement_id;

UPDATE relying_party_entitlement entitlement
JOIN relying_party_instance instance ON instance.id = entitlement.relying_party_instance_id
JOIN wallet_relying_party_service_entitlement service_entitlement
    ON service_entitlement.wallet_relying_party_service_id = instance.wallet_relying_party_service_id
   AND service_entitlement.entitlement =
       CONVERT(entitlement.entitlement USING utf8mb4) COLLATE utf8mb4_nopad_bin
SET entitlement.service_entitlement_id = service_entitlement.id;

SET @require_service_entitlement_id = CONCAT(
    'ALTER TABLE relying_party_entitlement MODIFY COLUMN service_entitlement_id VARCHAR(36) CHARACTER SET ',
    @service_entitlement_id_charset, ' COLLATE ', @service_entitlement_id_collation, ' NOT NULL'
);
PREPARE require_service_entitlement_id FROM @require_service_entitlement_id;
EXECUTE require_service_entitlement_id;
DEALLOCATE PREPARE require_service_entitlement_id;

ALTER TABLE relying_party_entitlement
    ADD CONSTRAINT fk_instance_service_entitlement FOREIGN KEY (service_entitlement_id)
        REFERENCES wallet_relying_party_service_entitlement (id) ON DELETE RESTRICT,
    ADD CONSTRAINT uq_instance_service_entitlement UNIQUE (relying_party_instance_id, service_entitlement_id),
    DROP INDEX constraint_no_duplicate_entitlements,
    DROP COLUMN entitlement;

DELETE configuration
FROM wallet_relying_party_service_credential_issuer_url configuration
JOIN wallet_relying_party_service_entitlement service_entitlement
    ON service_entitlement.id = configuration.id
   AND service_entitlement.wallet_relying_party_service_id = configuration.wallet_relying_party_service_id
   AND service_entitlement.entitlement = configuration.entitlement;
