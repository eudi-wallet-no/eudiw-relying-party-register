INSERT INTO `relying_party` (`uuid`, `name`, `orgno`, `public_sector`)
VALUES ('4000b045-0f64-455f-8446-545f46afa089', 'digdir',       '000000001', true),
       ('4e9dc882-3bc6-4822-aa01-4e0d2dc7d901', 'pizzabakeren', '000000002', false);

INSERT INTO `relying_party_entitlement` (`relying_party_uuid`, `entitlement`)
 VALUES ('4000b045-0f64-455f-8446-545f46afa089', -- digdir
         'entitlement-1'),
        ('4e9dc882-3bc6-4822-aa01-4e0d2dc7d901', -- pizzabakeren
         'entitlement-1'),
        ('4000b045-0f64-455f-8446-545f46afa089', -- digdir
         'entitlement-2');



INSERT INTO `relying_party_eaa` (`uuid`, `relying_party_uuid`, `namespace`, `intent`)
VALUES ('baf04533-588f-4106-a999-b2b9814522a4', -- PK for EEA1
        '4000b045-0f64-455f-8446-545f46afa089', -- digdir
        'namespace-1',
        'intent-1'),
       ('53a619d0-18b7-4a29-85a0-560486bb8b03', -- PK for EEA2
        '4e9dc882-3bc6-4822-aa01-4e0d2dc7d901', -- pizzabakeren
        'namespace-2',
        'intent-2');

INSERT INTO `relying_party_eaa_attribute` (`relying_party_eaa_uuid`, `attribute`, `intent`)
VALUES ('baf04533-588f-4106-a999-b2b9814522a4', -- FK to EEA1
        'attribute-1',
        'attribute-intent-1'),
       ('53a619d0-18b7-4a29-85a0-560486bb8b03', -- FK to EEA2
        'attribute-2',
        'attribute-intent-2');

-- more sample UUIDs:
-- '83403bb1-76fb-4ff6-98a8-61e2d1d3f8a2'
-- '49cbeda1-b96b-4072-bb0e-8655775c6a6c'
-- 'f98ca5ef-9c3b-46aa-8204-4c511307ee09'
-- '3def5b25-6f50-4df5-88a5-f727f2de983f'
-- 'e060b5f0-ab81-4e33-9431-1d8f179a00d1'
-- '333b1c62-e088-4a1f-b963-f476a617d396'
-- '5a3d5552-ca8d-43e4-a449-d066841e8cf7'
