-- store serial numbers as varchar
ALTER TABLE `issuer_certificate` MODIFY COLUMN `serial_no` VARCHAR(64) NOT NULL;
