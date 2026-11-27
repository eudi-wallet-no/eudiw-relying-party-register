DELETE certificate
FROM relying_party_certificates certificate
JOIN relying_party_instance instance ON instance.id = certificate.relying_party_instance_id
JOIN wallet_relying_party_service service ON service.id = instance.wallet_relying_party_service_id
JOIN wallet_relying_party party ON party.id = service.wallet_relying_party_id
WHERE party.active = FALSE OR service.active = FALSE;

DELETE instance
FROM relying_party_instance instance
JOIN wallet_relying_party_service service ON service.id = instance.wallet_relying_party_service_id
JOIN wallet_relying_party party ON party.id = service.wallet_relying_party_id
WHERE party.active = FALSE OR service.active = FALSE;

DELETE service
FROM wallet_relying_party_service service
JOIN wallet_relying_party party ON party.id = service.wallet_relying_party_id
WHERE party.active = FALSE OR service.active = FALSE;

DELETE FROM wallet_relying_party
WHERE active = FALSE;
