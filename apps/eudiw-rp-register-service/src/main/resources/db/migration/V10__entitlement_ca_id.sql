BEGIN;
ALTER TABLE `entitlement` ADD COLUMN `ca_id` VARCHAR(128);
UPDATE `entitlement`
SET `ca_id` = CASE `entitlement`
                         WHEN 'https://uri.etsi.org/19475/Entitlement/Service_Provider' THEN 'issuer'
                         WHEN 'https://uri.etsi.org/19475/Entitlement/QEAA_Provider'    THEN 'eaa_provider'
                         WHEN 'https://uri.etsi.org/19475/Entitlement/Non_Q_EAA_Provider' THEN 'eaa_provider'
                         WHEN 'https://uri.etsi.org/19475/Entitlement/PUB_EAA_Provider'  THEN 'pub_eaa_provider'
                         WHEN 'https://uri.etsi.org/19475/Entitlement/PID_Provider'      THEN 'pid_provider'
                         ELSE NULL
    END;
ALTER TABLE `entitlement` MODIFY `ca_id` VARCHAR(128) NOT NULL;
COMMIT;