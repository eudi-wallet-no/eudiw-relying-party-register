ALTER TABLE `wallet_relying_party`
    ADD COLUMN `country` VARCHAR(2) NOT NULL DEFAULT 'NO';
ALTER TABLE `wallet_relying_party`
    ADD COLUMN `trade_name` VARCHAR(255);
ALTER TABLE `wallet_relying_party`
    ADD COLUMN `provider_type` VARCHAR(64) NOT NULL DEFAULT 'WalletRelyingParty';

-- Additional identifiers only; orgno remains the national unique identifier in wallet_relying_party.orgno.
CREATE TABLE `wallet_relying_party_identifiers` (
    `wallet_relying_party_id` VARCHAR(36) NOT NULL,
    `position` INTEGER NOT NULL,
    `identifier_type` VARCHAR(2048) NOT NULL,
    `identifier_value` VARCHAR(255) NOT NULL,
    PRIMARY KEY (`wallet_relying_party_id`, `position`),
    CONSTRAINT `fk_wallet_relying_party_identifiers`
        FOREIGN KEY (`wallet_relying_party_id`) REFERENCES `wallet_relying_party` (`id`) ON DELETE CASCADE
);

CREATE TABLE `wallet_relying_party_policies` (
    `wallet_relying_party_id` VARCHAR(36) NOT NULL,
    `position` INTEGER NOT NULL,
    `policy_type` VARCHAR(2048) NOT NULL,
    `policy_uri` VARCHAR(2048) NOT NULL,
    PRIMARY KEY (`wallet_relying_party_id`, `position`),
    CONSTRAINT `fk_wallet_relying_party_policies`
        FOREIGN KEY (`wallet_relying_party_id`) REFERENCES `wallet_relying_party` (`id`) ON DELETE CASCADE
);

CREATE TABLE `wallet_relying_party_x5c` (
    `wallet_relying_party_id` VARCHAR(36) NOT NULL,
    `position` INTEGER NOT NULL,
    `certificate_chain` TEXT NOT NULL,
    PRIMARY KEY (`wallet_relying_party_id`, `position`),
    CONSTRAINT `fk_wallet_relying_party_x5c`
        FOREIGN KEY (`wallet_relying_party_id`) REFERENCES `wallet_relying_party` (`id`) ON DELETE CASCADE
);

CREATE TABLE `wallet_relying_party_postal_addresses` (
    `wallet_relying_party_id` VARCHAR(36) NOT NULL,
    `position` INTEGER NOT NULL,
    `postal_address` VARCHAR(2048) NOT NULL,
    PRIMARY KEY (`wallet_relying_party_id`, `position`),
    CONSTRAINT `fk_wallet_relying_party_postal_addresses`
        FOREIGN KEY (`wallet_relying_party_id`) REFERENCES `wallet_relying_party` (`id`) ON DELETE CASCADE
);

CREATE TABLE `wallet_relying_party_emails` (
    `wallet_relying_party_id` VARCHAR(36) NOT NULL,
    `position` INTEGER NOT NULL,
    `email` VARCHAR(320) NOT NULL,
    PRIMARY KEY (`wallet_relying_party_id`, `position`),
    CONSTRAINT `fk_wallet_relying_party_emails`
        FOREIGN KEY (`wallet_relying_party_id`) REFERENCES `wallet_relying_party` (`id`) ON DELETE CASCADE
);

CREATE TABLE `wallet_relying_party_phones` (
    `wallet_relying_party_id` VARCHAR(36) NOT NULL,
    `position` INTEGER NOT NULL,
    `phone` VARCHAR(64) NOT NULL,
    PRIMARY KEY (`wallet_relying_party_id`, `position`),
    CONSTRAINT `fk_wallet_relying_party_phones`
        FOREIGN KEY (`wallet_relying_party_id`) REFERENCES `wallet_relying_party` (`id`) ON DELETE CASCADE
);

CREATE TABLE `wallet_relying_party_info_uris` (
    `wallet_relying_party_id` VARCHAR(36) NOT NULL,
    `position` INTEGER NOT NULL,
    `info_uri` VARCHAR(2048) NOT NULL,
    PRIMARY KEY (`wallet_relying_party_id`, `position`),
    CONSTRAINT `fk_wallet_relying_party_info_uris`
        FOREIGN KEY (`wallet_relying_party_id`) REFERENCES `wallet_relying_party` (`id`) ON DELETE CASCADE
);

CREATE TABLE `supervisory_authority` (
    `id` VARCHAR(36) NOT NULL,
    `wallet_relying_party_id` VARCHAR(36) NOT NULL,
    `name` VARCHAR(255) NOT NULL,
    `country` VARCHAR(2) NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `uq_supervisory_authority_wallet_relying_party` UNIQUE (`wallet_relying_party_id`),
    CONSTRAINT `fk_supervisory_authority_wallet_relying_party`
        FOREIGN KEY (`wallet_relying_party_id`) REFERENCES `wallet_relying_party` (`id`) ON DELETE CASCADE
);

CREATE TABLE `supervisory_authority_emails` (
    `supervisory_authority_id` VARCHAR(36) NOT NULL,
    `position` INTEGER NOT NULL,
    `email` VARCHAR(320) NOT NULL,
    PRIMARY KEY (`supervisory_authority_id`, `position`),
    CONSTRAINT `fk_supervisory_authority_emails`
        FOREIGN KEY (`supervisory_authority_id`) REFERENCES `supervisory_authority` (`id`) ON DELETE CASCADE
);

CREATE TABLE `supervisory_authority_phones` (
    `supervisory_authority_id` VARCHAR(36) NOT NULL,
    `position` INTEGER NOT NULL,
    `phone` VARCHAR(64) NOT NULL,
    PRIMARY KEY (`supervisory_authority_id`, `position`),
    CONSTRAINT `fk_supervisory_authority_phones`
        FOREIGN KEY (`supervisory_authority_id`) REFERENCES `supervisory_authority` (`id`) ON DELETE CASCADE
);

CREATE TABLE `supervisory_authority_form_uris` (
    `supervisory_authority_id` VARCHAR(36) NOT NULL,
    `position` INTEGER NOT NULL,
    `form_uri` VARCHAR(2048) NOT NULL,
    PRIMARY KEY (`supervisory_authority_id`, `position`),
    CONSTRAINT `fk_supervisory_authority_form_uris`
        FOREIGN KEY (`supervisory_authority_id`) REFERENCES `supervisory_authority` (`id`) ON DELETE CASCADE
);

INSERT INTO `supervisory_authority` (`id`, `wallet_relying_party_id`, `name`, `country`)
SELECT `id`, `id`, 'Digitaliseringsdirektoratet', 'NO' FROM `wallet_relying_party`;
