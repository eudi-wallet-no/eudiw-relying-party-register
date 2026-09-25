package no.idporten.eudiw.trustlist.web;

/**
 * Mediatypar for tillitslister som ikkje finst i Spring sin MediaType.
 */
public final class TrustlistMediaTypes {

    public static final String APPLICATION_JOSE = "application/jose";
    public static final String APPLICATION_JOSE_JSON = "application/jose+json";
    /**
     * Ikkje-standard, ikkje registrert hos IANA. Brukt av enkelte LoTE-klientar.
     */
    public static final String APPLICATION_VND_LOTE_JSON = "application/vnd.lote+json";
    public static final String APPLICATION_VND_ETSI_TSL_XML = "application/vnd.etsi.tsl+xml";

    private TrustlistMediaTypes() {
    }

}
