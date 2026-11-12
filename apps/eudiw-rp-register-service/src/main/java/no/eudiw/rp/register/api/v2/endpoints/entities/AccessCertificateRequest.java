package no.eudiw.rp.register.api.v2.endpoints.entities;

import no.eudiw.rp.register.api.v1.resource.certificates.PKCS10CertificationRequestJsonDeserializer;
import no.eudiw.rp.register.api.v1.resource.certificates.PKCS10CertificationRequestJsonSerializer;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;

public record AccessCertificateRequest(
    @JsonSerialize(using = PKCS10CertificationRequestJsonSerializer.class)
    @JsonDeserialize(using = PKCS10CertificationRequestJsonDeserializer.class)
    PKCS10CertificationRequest csr
) { }
