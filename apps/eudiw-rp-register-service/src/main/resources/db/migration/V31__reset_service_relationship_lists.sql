UPDATE `wallet_relying_party_service`
SET `uses_intermediaries` = JSON_ARRAY(),
    `served_wrp_services` = JSON_ARRAY();
