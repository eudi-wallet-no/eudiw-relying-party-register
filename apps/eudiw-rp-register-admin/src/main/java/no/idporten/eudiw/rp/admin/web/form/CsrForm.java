package no.idporten.eudiw.rp.admin.web.form;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import no.idporten.eudiw.rp.admin.service.accesscertificates.PKCS10CertificationRequestConverter;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;

@Getter
@NoArgsConstructor
@EqualsAndHashCode
public class CsrForm {

    private PKCS10CertificationRequest csr;

    private String csrField;

    @SuppressWarnings("unused")
    public void setCsrField(String csrStr) {
        this.csrField = csrStr;
        this.csr = PKCS10CertificationRequestConverter.fromString(csrStr);
    }

    public static CsrForm empty() {
        return new CsrForm();
    }
}
