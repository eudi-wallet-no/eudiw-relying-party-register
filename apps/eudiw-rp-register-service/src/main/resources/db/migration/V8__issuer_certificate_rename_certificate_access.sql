CREATE TABLE IF NOT EXISTS `issuer_certificate`
(
    `id`                VARCHAR(36)     NOT NULL,
    `entitlement_id`    VARCHAR(36)     NOT NULL,
    `issuer`            VARCHAR(255)    NOT NULL,
    `ca_id`             VARCHAR(255)     NOT NULL,
    `certificate_pem`   VARCHAR(8192)   NOT NULL,
    `subject_dn`        VARCHAR(255)    NOT NULL,
    `serial_no`         BIGINT          NOT NULL UNIQUE,
    `valid_from_ms`     BIGINT          NOT NULL DEFAULT(0),
    `valid_until_ms`    BIGINT          NOT NULL DEFAULT(0),

    PRIMARY KEY (`id`),
    CONSTRAINT `fk_entitlement_issuer_certificate`
    FOREIGN KEY (`entitlement_id`)
    REFERENCES `relying_party_entitlement` (`id`)
    ON DELETE CASCADE
    );

ALTER TABLE IF EXISTS `relying_party_certificate`
    RENAME TO `access_certificate`;

ALTER TABLE IF EXISTS `access_certificate`
    ADD COLUMN `issuer` VARCHAR(255);

ALTER TABLE IF EXISTS `access_certificate`
    ADD COLUMN `ca_id` VARCHAR(255);

UPDATE `access_certificate`
SET `issuer` = 'CN=eidas2sandkasse.net root CA test, OU=Digdir, C=no, organizationIdentifier=NTRNO-991825827'
WHERE `issuer` IS NULL;

UPDATE `access_certificate`
SET `ca_id` = 'access'
WHERE `ca_id` IS NULL;

ALTER TABLE IF EXISTS `access_certificate`
    MODIFY COLUMN `issuer` VARCHAR(255) NOT NULL;

ALTER TABLE IF EXISTS `access_certificate`
    MODIFY COLUMN `ca_id` VARCHAR(255) NOT NULL;