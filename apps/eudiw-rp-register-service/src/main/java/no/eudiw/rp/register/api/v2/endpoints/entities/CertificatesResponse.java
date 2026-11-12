package no.eudiw.rp.register.api.v2.endpoints.entities;

import java.util.List;

public record CertificatesResponse(List<CertificateResponse> certificates) {
    public static CertificatesResponse from(List<CertificateResponse> certificates) {
        return new CertificatesResponse(certificates);
    }
}
