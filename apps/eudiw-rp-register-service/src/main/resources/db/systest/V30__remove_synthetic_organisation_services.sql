-- EUW-2026: Remove services without certificates for the synthetic organisation in systest.
START TRANSACTION;

-- Lock the organisation during cleanup.
SELECT id FROM wallet_relying_party WHERE orgno = '370452490' FOR UPDATE;

-- Delete instances only when their entire service has no access or issuer certificates.
DELETE FROM relying_party_instance
WHERE wallet_relying_party_service_id IN (
    SELECT service.id
    FROM wallet_relying_party_service service
    JOIN wallet_relying_party party ON party.id = service.wallet_relying_party_id
    WHERE party.orgno = '370452490'
      AND NOT EXISTS (
          SELECT 1
          FROM relying_party_certificates certificate
          LEFT JOIN relying_party_entitlement entitlement ON entitlement.id = certificate.entitlement_id
          JOIN relying_party_instance certified_instance
              ON certified_instance.id = COALESCE(certificate.relying_party_instance_id, entitlement.relying_party_instance_id)
          WHERE certified_instance.wallet_relying_party_service_id = service.id
      )
);

-- Delete the organisation's services that have no remaining instances.
DELETE FROM wallet_relying_party_service
WHERE wallet_relying_party_id IN (
    SELECT id FROM wallet_relying_party WHERE orgno = '370452490'
)
  AND NOT EXISTS (
      SELECT 1
      FROM relying_party_instance instance
      WHERE instance.wallet_relying_party_service_id = wallet_relying_party_service.id
  );

COMMIT;
