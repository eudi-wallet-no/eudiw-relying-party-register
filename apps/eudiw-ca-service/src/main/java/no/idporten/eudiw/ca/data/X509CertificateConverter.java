package no.idporten.eudiw.ca.data;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import no.idporten.eudiw.ca.util.CertificateEncodingUtils;
import org.bouncycastle.cert.X509CertificateHolder;

import java.security.cert.X509Certificate;

@Converter
public class X509CertificateConverter implements AttributeConverter<X509Certificate, String> {

    @Override
    public String convertToDatabaseColumn(X509Certificate certificate) {
        return CertificateEncodingUtils.encodeToPem(certificate);
    }

    @Override
    public X509Certificate convertToEntityAttribute(String certificate) {
        X509CertificateHolder certificateHolder = CertificateEncodingUtils.decodeFromPem(certificate, X509CertificateHolder.class);
        return CertificateEncodingUtils.toX509Certificate(certificateHolder);
    }

}
