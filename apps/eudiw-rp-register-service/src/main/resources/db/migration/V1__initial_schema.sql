CREATE TABLE IF NOT EXISTS `relying_party`
(
    `id`                 UUID NOT NULL,

    `orgno`              VARCHAR(9) NOT NULL UNIQUE,
    `name`               VARCHAR(255) NOT NULL,

    `public_sector`      BOOLEAN NOT NULL,

    `created_ms`         BIGINT NOT NULL DEFAULT (0),
    `last_updated_ms`    BIGINT NOT NULL DEFAULT (0),
    `active`             BOOLEAN NOT NULL DEFAULT (TRUE),

    PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS `relying_party_entitlement`
(
    `id`                  UUID NOT NULL,
    `relying_party_id`    UUID NOT NULL,

    `entitlement`         VARCHAR(255) NOT NULL,

    PRIMARY KEY (`id`),

    CONSTRAINT `fk_relying_party_entitlement`
        FOREIGN KEY (`relying_party_id`)
        REFERENCES `relying_party` (`id`)
        ON DELETE CASCADE,

    CONSTRAINT `constraint_no_duplicate_entitlements`
        UNIQUE (`relying_party_id`, `entitlement`)
);

CREATE TABLE IF NOT EXISTS `relying_party_eaa`
(
    `id`                  UUID NOT NULL,
    `relying_party_id`    UUID NOT NULL,

    `namespace`           VARCHAR(255) NOT NULL,
    `intent`              VARCHAR(255) NOT NULL,

    PRIMARY KEY (`id`),

    CONSTRAINT `fk_relying_party_eaa`
        FOREIGN KEY (`relying_party_id`)
        REFERENCES `relying_party` (`id`)
        ON DELETE CASCADE
);
