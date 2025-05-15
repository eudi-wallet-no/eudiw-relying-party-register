CREATE TABLE IF NOT EXISTS `certificate`
(
    `serial_no`         BIGINT NOT NULL,
    `issuer_ca`         VARCHAR(255) NOT NULL,
    `certificate`       VARCHAR(8192) NOT NULL,
    `valid_from_ms`     BIGINT NOT NULL DEFAULT(0),
    `valid_until_ms`    BIGINT NOT NULL DEFAULT(0),
    `revoked_at_ms`     BIGINT DEFAULT(0),
    `revocation_reason` INT DEFAULT(-1),
    PRIMARY KEY (`serial_no`, `issuer_ca`)
);
