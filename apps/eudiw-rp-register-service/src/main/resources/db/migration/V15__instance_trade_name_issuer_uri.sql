CREATE TABLE `legal_entity` (
    `id`                 VARCHAR(36)     NOT NULL,
    `name`               VARCHAR(255)   NOT NULL,
    `orgno`              VARCHAR(9)    NOT NULL UNIQUE,
    `public_sector`      BOOLEAN NOT NULL DEFAULT (TRUE),
    `created_ms`         BIGINT NOT NULL DEFAULT (0),
    `last_updated_ms`    BIGINT NOT NULL DEFAULT (0),
    `active`             BOOLEAN NOT NULL DEFAULT (TRUE),

    PRIMARY KEY (`id`)
);

INSERT INTO `legal_entity` (
    `id`,
    `name`,
    `orgno`,
    `public_sector`,
    `created_ms`,
    `last_updated_ms`,
    `active`
)
SELECT
    UUID(),
    `name`,
    `orgno`,
    `public_sector`,
    `created_ms`,
    `last_updated_ms`,
    `active`
FROM (
     SELECT *, ROW_NUMBER() OVER (PARTITION BY `orgno` ORDER BY `created_ms`) AS `row_number`
     FROM `relying_party`
     WHERE `active` = TRUE AND `deleted` = FALSE) as `relying_party_by_orgno`
WHERE `relying_party_by_orgno`.`row_number` = 1;

ALTER TABLE `relying_party` RENAME TO `relying_party_instance`;

ALTER TABLE `relying_party_entitlement`
DROP FOREIGN KEY `fk_relying_party_entitlement`;

ALTER TABLE `relying_party_eaa`
DROP FOREIGN KEY `fk_relying_party_eaa`;

ALTER TABLE `access_certificate`
DROP FOREIGN KEY `fk_relying_party_certificate`;

ALTER TABLE `relying_party_entitlement`
    CHANGE COLUMN `relying_party_id` `relying_party_instance_id` VARCHAR(36) NOT NULL;

ALTER TABLE `relying_party_eaa`
    CHANGE COLUMN `relying_party_id` `relying_party_instance_id` VARCHAR(36) NOT NULL;

ALTER TABLE `access_certificate`
    CHANGE COLUMN `relying_party_id` `relying_party_instance_id` VARCHAR(36) NOT NULL;

ALTER TABLE `relying_party_entitlement`
    ADD CONSTRAINT `fk_relying_party_instance_entitlement`
        FOREIGN KEY (`relying_party_instance_id`)
            REFERENCES `relying_party_instance` (`id`)
            ON DELETE CASCADE;

ALTER TABLE `relying_party_eaa`
    ADD CONSTRAINT `fk_relying_party_instance_eaa`
        FOREIGN KEY (`relying_party_instance_id`)
            REFERENCES `relying_party_instance` (`id`)
            ON DELETE CASCADE;

ALTER TABLE `access_certificate`
    ADD CONSTRAINT `fk_relying_party_instance_certificate`
        FOREIGN KEY (`relying_party_instance_id`)
            REFERENCES `relying_party_instance` (`id`)
            ON DELETE CASCADE;

ALTER TABLE `relying_party_instance`
    ADD COLUMN `legal_entity_id` VARCHAR(36);

UPDATE `relying_party_instance` `rpi`
SET `legal_entity_id` = (
    SELECT `le`.`id`
    FROM `legal_entity` `le`
    WHERE `le`.`orgno` = `rpi`.`orgno`
)
WHERE `rpi`.`legal_entity_id` IS NULL;

ALTER TABLE `relying_party_instance`
    ADD CONSTRAINT `fk_rp_instance`
        FOREIGN KEY (`legal_entity_id`)
            REFERENCES `legal_entity`(`id`);

ALTER TABLE `relying_party_instance`
    DROP COLUMN `deleted`;

ALTER TABLE `relying_party_instance`
    DROP COLUMN `orgno`;

ALTER TABLE `relying_party_instance`
    DROP COLUMN `public_sector`;

ALTER TABLE `relying_party_instance`
    RENAME COLUMN `name` TO `trade_name`;

ALTER TABLE `relying_party_entitlement`
    ADD COLUMN `credential_issuer_url` VARCHAR(255);