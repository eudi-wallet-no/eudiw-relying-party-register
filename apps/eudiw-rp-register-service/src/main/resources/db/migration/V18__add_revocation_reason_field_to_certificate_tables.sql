ALTER TABLE `access_certificate`
    ADD COLUMN `revocation_status` INTEGER NOT NULL DEFAULT(-1);

ALTER TABLE `issuer_certificate`
    ADD COLUMN `revocation_status` INTEGER NOT NULL DEFAULT(-1);
