package no.idporten.eudiw.rp.admin.integrations.issuermetadata;

public class IssuerMetadataException extends RuntimeException {

    public IssuerMetadataException(String message) {
        super(message);
    }

    public IssuerMetadataException(String message, Throwable cause) {
        super(message, cause);
    }
}
