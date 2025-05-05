INSERT INTO `relying_party` (`id`, `name`, `orgno`, `public_sector`, `created_ms`, `last_updated_ms`, `active`)
VALUES ('4000b045-0f64-455f-8446-545f46afa089', 'digdir',       '123456785', true, 1, 1, false),
       ('4e9dc882-3bc6-4822-aa01-4e0d2dc7d901', 'pizzabakeren', '985917957', false, 2, 3, true);

INSERT INTO `relying_party_entitlement` (`id`, `relying_party_id`, `entitlement`)
 VALUES ('83403bb1-76fb-4ff6-98a8-61e2d1d3f8a2',
         '4000b045-0f64-455f-8446-545f46afa089', -- digdir
         'entitlement-1'),
        ('49cbeda1-b96b-4072-bb0e-8655775c6a6c',
         '4e9dc882-3bc6-4822-aa01-4e0d2dc7d901', -- pizzabakeren
         'entitlement-1'),
        ('f98ca5ef-9c3b-46aa-8204-4c511307ee09',
         '4000b045-0f64-455f-8446-545f46afa089', -- digdir
         'entitlement-2');

INSERT INTO `relying_party_eaa` (`id`, `relying_party_id`, `namespace`, `intent`)
VALUES ('baf04533-588f-4106-a999-b2b9814522a4', -- PK for EEA1
        '4000b045-0f64-455f-8446-545f46afa089', -- digdir
        'namespace-1',
        'intent-1'),
       ('53a619d0-18b7-4a29-85a0-560486bb8b03', -- PK for EEA2
        '4e9dc882-3bc6-4822-aa01-4e0d2dc7d901', -- pizzabakeren
        'namespace-2',
        'intent-2');

INSERT INTO `relying_party_access_certificate` (`certificate_pem`, `subject_dn`, `serial_no`, `id`, `relying_party_id`)
VALUES (
'-----BEGIN CERTIFICATE-----
MIIEnDCCAoSgAwIBAgIJAO5izNutMzG1MA0GCSqGSIb3DQEBCwUAMGcxGDAWBgNV
BGETD05UUk5PLTk5MTgyNTgyNzELMAkGA1UEBhMCbm8xDzANBgNVBAsTBkRpZ2Rp
cjEtMCsGA1UEAxMkZWlkYXMyc2FuZGthc3NlIFJQIEFjY2VzcyBDQSBzeXN0ZXN0
MB4XDTI1MDQyOTEwMjQzN1oXDTI1MDgwNzEwMjQzN1owTTELMAkGA1UEBhMCZGsx
ETAPBgNVBAsTCGJhbmFuLmRrMREwDwYDVQQDEwhiYW5hbi5kazEYMBYGA1UEYQwP
TlRSTk8tOTc0NzIwNzYwMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEvXhASuAv
GrB05RjcGiCORFzujiGM1Rg0PZHBQkLMJIbfls/3ikZP4PK/CJ8cdd/9o0Tg1EPt
vb4YSGZK5nWXNqOCAS4wggEqMB8GA1UdIwQYMBaAFNQ3MBWz2Oky0K8eK07ujN2t
yJYFMB0GA1UdDgQWBBRm1qXMivlPK1iL+yhfSDoAfJqG7zAMBgNVHRMBAf8EAjAA
MFEGA1UdHwRKMEgwRqBEoEKGQGh0dHBzOi8vY2EuZWlkYXMyc2FuZGthc3NlLmRl
di92MS9jZXJ0cy9pbnRlcm1lZGlhdGVzL2FjY2Vzcy5jcmwwDgYDVR0PAQH/BAQD
AgWgMFwGCCsGAQUFBwEBBFAwTjBMBggrBgEFBQcwAoZAaHR0cHM6Ly9jYS5laWRh
czJzYW5ka2Fzc2UuZGV2L3YxL2NlcnRzL2ludGVybWVkaWF0ZXMvYWNjZXNzLmNl
cjAZBgNVHREEEjAQgg5zdG9yZS5iYW5hbi5kazANBgkqhkiG9w0BAQsFAAOCAgEA
YNykHvV1JGjvML2yn7EgijUZKCcIp4w7ddgNUweDMSkS4GMBk8NZcMYxkQe7NWVE
4bViq8fYdHg5ZoBdEr1kV+TOdM18T1W400rBEgwnii9WSQFA7CR3Cr2FW7lyXpk1
lb04mEaZlzMAosC/sEwBM422he0MIx/IfEmcNhGWsjdh3cdZILnlsG5siKQa2WpN
zTZo26Dgt2D4RW9kkmqJ2A8ZuCTWPWzzdvxOmvf95xof9D0Pk27B8x3Ee6d8CKD5
GAAXYSI68t+xxdKQcEJlfmEeXkDzAC9L7e2Js/dcqsSzQJVwLv+c5pk7gpwgIuT6
3I738yUiMWjg6tjtb7YBcAKNHmc00tyBiLrfLolKIUMIiFfG74MLmHtY0QPc3psc
5fY6nlv3mNGncjK47vTvbWDoxiaOUSOapn5ixJqoQZO6VoLN/Q6efh/HL2f8vbWQ
afudy2QZH+/XgcqpUeUppA3JX1EuT+xTvv/eljZhtIJTOZZhtCtP0fNv+p+ISpQr
niQSxZyNhzOcr9/udER/J+mHmBmr2VI89yxd3boaDmjyS/xSwe0RTkbGBYamQKXI
VyzXkYq8hjfBmkoBl5thbOL+Ljtl5umBQgINlckv4ja32NlUExdpVb373DAVQEue
51lWt3rvdU/13W1BYHzeB8EJ4IL+28NqOD36FdV6Vpo=
-----END CERTIFICATE-----',
    '2.5.4.97=#0c0f4e54524e4f2d393734373230373630,CN=banan.dk,OU=banan.dk,C=dk',
    1717751717,
    '3def5b25-6f50-4df5-88a5-f727f2de983f',
    '4000b045-0f64-455f-8446-545f46afa089'
       );


-- -- more sample UUIDs:
-- -- '3def5b25-6f50-4df5-88a5-f727f2de983f'
-- -- 'e060b5f0-ab81-4e33-9431-1d8f179a00d1'
-- -- '333b1c62-e088-4a1f-b963-f476a617d396'
-- -- '5a3d5552-ca8d-43e4-a449-d066841e8cf7'
