CREATE TABLE `legal_person` (
    `id` VARCHAR(36) NOT NULL,
    `wallet_relying_party_id` VARCHAR(36) NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `uq_legal_person_wallet_relying_party` UNIQUE (`wallet_relying_party_id`),
    CONSTRAINT `fk_legal_person_wallet_relying_party`
        FOREIGN KEY (`wallet_relying_party_id`) REFERENCES `wallet_relying_party` (`id`)
);

CREATE TABLE `legal_person_legal_names` (
    `legal_person_id` VARCHAR(36) NOT NULL,
    `position` INTEGER NOT NULL,
    `legal_name` VARCHAR(255) NOT NULL,
    PRIMARY KEY (`legal_person_id`, `position`),
    CONSTRAINT `fk_legal_person_legal_names`
        FOREIGN KEY (`legal_person_id`) REFERENCES `legal_person` (`id`) ON DELETE CASCADE
);

CREATE TABLE `legal_person_established_by_law` (
    `legal_person_id` VARCHAR(36) NOT NULL,
    `position` INTEGER NOT NULL,
    `legislative_identifier` VARCHAR(2048) NOT NULL,
    `legal_basis` VARCHAR(2048),
    PRIMARY KEY (`legal_person_id`, `position`),
    CONSTRAINT `fk_legal_person_established_by_law`
        FOREIGN KEY (`legal_person_id`) REFERENCES `legal_person` (`id`) ON DELETE CASCADE
);

INSERT INTO `legal_person` (`id`, `wallet_relying_party_id`)
SELECT `id`, `id` FROM `wallet_relying_party`;

INSERT INTO `legal_person_legal_names` (`legal_person_id`, `position`, `legal_name`)
SELECT `id`, 0, `legal_name`
FROM `wallet_relying_party`
WHERE `legal_name` IS NOT NULL;
