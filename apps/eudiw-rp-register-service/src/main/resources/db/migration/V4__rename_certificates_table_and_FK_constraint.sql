ALTER TABLE IF EXISTS `relying_party_access_certificate`
RENAME TO `relying_party_certificate`;

ALTER TABLE `relying_party_certificate`
DROP CONSTRAINT `fk_relying_party_access_certificate`;

ALTER TABLE `relying_party_certificate`
ADD CONSTRAINT `fk_relying_party_certificate`
    FOREIGN KEY (`relying_party_id`)
    REFERENCES `relying_party` (`id`)
    ON DELETE CASCADE;
