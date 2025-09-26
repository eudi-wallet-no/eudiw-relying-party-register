BEGIN;
ALTER TABLE `entitlement` ADD COLUMN `display_name` VARCHAR(128);
UPDATE `entitlement`
SET `display_name` = CASE `entitlement`
                         WHEN 'https://uri.etsi.org/19475/Entitlement/Service_Provider' THEN 'Service Provider'
                         WHEN 'https://uri.etsi.org/19475/Entitlement/QEAA_Provider'    THEN 'QEAA Provider'
                         WHEN 'https://uri.etsi.org/19475/Entitlement/Non_Q_EAA_Provider' THEN 'Non Q EAA Provider'
                         WHEN 'https://uri.etsi.org/19475/Entitlement/PUB_EAA_Provider'  THEN 'PUB EAA Provider'
                         WHEN 'https://uri.etsi.org/19475/Entitlement/PID_Provider'      THEN 'PID Provider'
                         ELSE NULL
    END;
ALTER TABLE `entitlement` MODIFY `display_name` VARCHAR(128) NOT NULL;
COMMIT;