-- store serial numbers as varchar
ALTER TABLE `certificate` MODIFY COLUMN `serial_no` VARCHAR(64) NOT NULL;
