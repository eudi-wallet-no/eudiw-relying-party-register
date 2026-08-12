ALTER TABLE `relying_party` DROP COLUMN `orgno`;
ALTER TABLE `relying_party` ADD COLUMN `orgno` VARCHAR(9) NOT NULL;
