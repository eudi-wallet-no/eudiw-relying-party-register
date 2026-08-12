-- store serial numbers as varchar
ALTER TABLE `relying_party_certificate` MODIFY COLUMN `serial_no` VARCHAR(64) NOT NULL;
