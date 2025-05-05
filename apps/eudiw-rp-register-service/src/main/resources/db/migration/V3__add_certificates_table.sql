CREATE TABLE IF NOT EXISTS `relying_party_access_certificate`
(
    `id`               VARCHAR(36) NOT NULL,
    `relying_party_id` VARCHAR(36) NOT NULL,

    `certificate_pem`  VARCHAR(65535) NOT NULL,
    `subject_dn`       VARCHAR(255)   NOT NULL,
    `serial_no`        BIGINT         NOT NULL UNIQUE,
    `valid_from_ms`    BIGINT         NOT NULL DEFAULT(0),
    `valid_until_ms`   BIGINT         NOT NULL DEFAULT(0),

    PRIMARY KEY (`id`),
    CONSTRAINT `fk_relying_party_access_certificate`
        FOREIGN KEY (`relying_party_id`)
        REFERENCES `relying_party` (`id`)
        ON DELETE CASCADE
);
