UPDATE `issuer_certificate`
SET `ca_id` = (
    SELECT DISTINCT `e`.`ca_id`
    FROM `issuer_certificate` `ic`
             JOIN `relying_party_entitlement` `rpe`
                  ON `ic`.`entitlement_id` = `rpe`.`id`
             JOIN `entitlement` `e`
                  ON `rpe`.`entitlement` = `e`.`entitlement`
);

UPDATE `access_certificate` SET `ca_id` = 'access';
