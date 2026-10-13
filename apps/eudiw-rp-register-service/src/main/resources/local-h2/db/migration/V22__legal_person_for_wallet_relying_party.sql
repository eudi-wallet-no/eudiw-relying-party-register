-- The legal name stays in wallet_relying_party.legal_name; legal_person holds the remaining TS2 LegalPerson data.
CREATE TABLE `legal_person` (
    `id` VARCHAR(36) NOT NULL,
    `wallet_relying_party_id` VARCHAR(36) NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `uq_legal_person_wallet_relying_party` UNIQUE (`wallet_relying_party_id`),
    CONSTRAINT `fk_legal_person_wallet_relying_party`
        FOREIGN KEY (`wallet_relying_party_id`) REFERENCES `wallet_relying_party` (`id`) ON DELETE CASCADE
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
