BEGIN NOT ATOMIC
    IF NOT EXISTS (
        SELECT 1
        FROM flyway_schema_history
        WHERE version = '37'
          AND success = 1
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Cannot drop the legacy service issuer URL table before V37 succeeds';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM wallet_relying_party_service_credential_issuer_url
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Cannot drop the legacy service issuer URL table while it contains rows';
    END IF;
END;

DROP TABLE wallet_relying_party_service_credential_issuer_url;
