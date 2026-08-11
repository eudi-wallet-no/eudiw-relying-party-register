package no.idporten.eudiw.rp.admin.service.exception;

import lombok.Getter;

@Getter
public class CertificateNotFoundException extends NotFoundException {

    private final String rpId;
    private final String certificateId;
    public CertificateNotFoundException(String errorDescription, String rpId, String certificateId) {
        super(errorDescription);
        this.rpId = rpId;
        this.certificateId = certificateId;
    }
}
