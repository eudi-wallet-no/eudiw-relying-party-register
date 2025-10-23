BEGIN;
UPDATE `entitlement`
SET `display_name` = CASE `entitlement`
    WHEN 'https://uri.etsi.org/19475/Entitlement/QEAA_Provider'      THEN 'Qualified EAA Provider'
    WHEN 'https://uri.etsi.org/19475/Entitlement/Non_Q_EAA_Provider' THEN 'Non-qualified EAA Provider'
    WHEN 'https://uri.etsi.org/19475/Entitlement/PUB_EAA_Provider'   THEN 'Public EAA Provider'
    ELSE `display_name`
END;
COMMIT;
