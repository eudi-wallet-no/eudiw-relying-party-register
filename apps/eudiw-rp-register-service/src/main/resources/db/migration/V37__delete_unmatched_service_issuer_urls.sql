-- V36 moved matching URLs to the service roles. Keep the table readable for V36 during rollout.
DELETE FROM wallet_relying_party_service_credential_issuer_url;
