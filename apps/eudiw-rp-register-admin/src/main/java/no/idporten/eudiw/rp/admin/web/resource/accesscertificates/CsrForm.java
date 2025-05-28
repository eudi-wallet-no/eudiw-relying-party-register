package no.idporten.eudiw.rp.admin.web.resource.accesscertificates;

import org.bouncycastle.pkcs.PKCS10CertificationRequest;

public record CsrForm(
    PKCS10CertificationRequest csr
) {
    public static CsrForm empty() {
        return new CsrForm(null);
    }
}
