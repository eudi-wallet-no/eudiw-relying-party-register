ALTER TABLE wallet_relying_party_service
    ADD COLUMN status VARCHAR(16) NULL,
    ADD COLUMN created_at DATE NULL,
    ADD COLUMN suspended_at DATE NULL,
    ADD COLUMN revoked_at DATE NULL;

UPDATE wallet_relying_party_service
SET status = CASE WHEN active THEN 'active' ELSE 'suspended' END,
    created_at = DATE_ADD('1970-01-01', INTERVAL FLOOR(created_ms / 86400000) DAY);

ALTER TABLE wallet_relying_party_service
    MODIFY COLUMN status VARCHAR(16) NOT NULL,
    MODIFY COLUMN created_at DATE NOT NULL,
    ADD CONSTRAINT chk_wallet_relying_party_service_status
        CHECK (status IN ('active', 'suspended', 'revoked'));
