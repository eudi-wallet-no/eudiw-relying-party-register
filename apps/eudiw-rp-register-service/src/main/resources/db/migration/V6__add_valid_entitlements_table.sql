CREATE TABLE IF NOT EXISTS `entitlement`
(
    `id`            VARCHAR(36) NOT NULL,
    `entitlement`   VARCHAR(256) NOT NULL UNIQUE,
    `active`        BOOLEAN NOT NULL DEFAULT (TRUE),

    PRIMARY KEY (`id`)
);

INSERT INTO `entitlement` (`id`, `entitlement`, `active`)
VALUES  ('7b528989-04f8-4092-bac1-bb1073f9ba17',
         'https://uri.etsi.org/19475/Entitlement/Service_Provider',
         'true'),
        ('da7ba848-5e69-4e65-9817-e1e78a0db31c',
         'https://uri.etsi.org/19475/Entitlement/QEAA_Provider',
         'true'),
        ('7e7ecbdf-6546-4e5e-9659-fd9dacdb09a1',
         'https://uri.etsi.org/19475/Entitlement/Non_Q_EAA_Provider',
         'true'),
        ('2379321f-f152-4a6d-96c1-63f0c0ac2d83',
         'https://uri.etsi.org/19475/Entitlement/PUB_EAA_Provider',
         'true'),
        ('2471828a-73ff-4550-9b4e-90fb13741a88',
         'https://uri.etsi.org/19475/Entitlement/PID_Provider',
         'true')
;