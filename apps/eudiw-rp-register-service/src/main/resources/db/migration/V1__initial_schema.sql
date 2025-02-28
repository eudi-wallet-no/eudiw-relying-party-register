-- [1] https://digdir.atlassian.net/wiki/x/OwDXsg
-- [2] https://data.brreg.no/enhetsregisteret/api/dokumentasjon/no/index.html#tag/Enheter/operation/lastnedEnheter

CREATE TABLE IF NOT EXISTS `relying_party`
(
    -- TODO: Jira EUW-26 (1) (https://digdir.atlassian.net/browse/EUW-26)
    `uuid`             VARCHAR(36) NOT NULL DEFAULT (UUID()),

    -- TODO: Jira EUW-26 (2, 2.1) (https://digdir.atlassian.net/browse/EUW-26)
    `orgno`            VARCHAR(9) NOT NULL UNIQUE, -- see [2]
    `name`             VARCHAR(180) NOT NULL, -- see [2]

    `public_sector`    BOOLEAN NOT NULL,

    PRIMARY KEY (`uuid`)
);

CREATE TABLE IF NOT EXISTS `relying_party_entitlement`
(
    `uuid`                  VARCHAR(36) NOT NULL DEFAULT (UUID()),
    `relying_party_uuid`    VARCHAR(36) NOT NULL,

    -- TODO: Jira EUW-26 (2, 2.2) (https://digdir.atlassian.net/browse/EUW-26)
    `entitlement`           VARCHAR(255) NOT NULL,

    PRIMARY KEY (`uuid`),
    CONSTRAINT `FK_relying_party_entitlement`
        FOREIGN KEY (`relying_party_uuid`)
        REFERENCES `relying_party` (`uuid`)
        ON DELETE CASCADE,

    CONSTRAINT `constraint_no_duplicate_entitlements`
        UNIQUE (`relying_party_uuid`, `entitlement`)
);

CREATE TABLE IF NOT EXISTS `relying_party_eaa`
(
    -- TODO: Jira EUW-26 (1) (https://digdir.atlassian.net/browse/EUW-26)
    `uuid`                  VARCHAR(36) NOT NULL DEFAULT (UUID()),
    `relying_party_uuid`    VARCHAR(36) NOT NULL,

    -- TODO: Jira EUW-26 (2) (https://digdir.atlassian.net/browse/EUW-26)
    `namespace`             VARCHAR(255) NOT NULL,
    `intent`                VARCHAR(255) NOT NULL,

    PRIMARY KEY (`uuid`),
    CONSTRAINT `fk_relying_party_eaa`
        FOREIGN KEY (`relying_party_uuid`)
        REFERENCES `relying_party` (`uuid`)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS `relying_party_eaa_attribute`
(
    -- TODO: Jira EUW-26 (1, 3) (https://digdir.atlassian.net/browse/EUW-26)
    `uuid`                      VARCHAR(36) NOT NULL DEFAULT (UUID()),
    `relying_party_eaa_uuid`    VARCHAR(36) NOT NULL,

    -- TODO: Jira EUW-26 (2) (https://digdir.atlassian.net/browse/EUW-26)
    `attribute`                 VARCHAR(255) NOT NULL,
    `intent`                    VARCHAR(255),

    PRIMARY KEY (`uuid`),
    CONSTRAINT `fk_relying_party_eaa_attribute`
        FOREIGN KEY (`relying_party_eaa_uuid`)
        REFERENCES `relying_party_eaa` (`uuid`)
        ON DELETE CASCADE
);
