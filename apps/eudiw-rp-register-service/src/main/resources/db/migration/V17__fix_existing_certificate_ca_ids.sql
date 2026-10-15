UPDATE issuer_certificate ic
    INNER JOIN relying_party_entitlement rpe
        ON ic.entitlement_id = rpe.id
    INNER JOIN entitlement e
        ON rpe.entitlement = e.entitlement
    SET ic.ca_id = e.ca_id;

UPDATE `access_certificate` SET `ca_id` = 'access';
