package no.idporten.eudiw.rp.admin.web.form;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import no.idporten.eudiw.rp.admin.service.accesscertificates.PKCS10CertificationRequestConverter;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;

import java.util.List;

@Getter
@NoArgsConstructor
@EqualsAndHashCode
public class CsrIssuerForm {

    private PKCS10CertificationRequest csr;

    private String csrField;

    @Setter
    private String entitlement;

    @SuppressWarnings("unused")
    public void setCsrField(String csrStr) {
        this.csrField = csrStr;
        this.csr = PKCS10CertificationRequestConverter.fromString(csrStr);
    }

    public static CsrIssuerForm empty() {
        return new CsrIssuerForm();
    }
}
