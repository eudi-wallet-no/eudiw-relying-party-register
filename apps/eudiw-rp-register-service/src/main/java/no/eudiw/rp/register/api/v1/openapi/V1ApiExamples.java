package no.eudiw.rp.register.api.v1.openapi;

public class V1ApiExamples {
    private V1ApiExamples() { }

    public static final String NOT_FOUND_ERROR_EXAMPLE =
        """
        {"error": "not_found", "error_description": "<description of the error>"}""";
    public static final String BAD_REQUEST_ERROR_EXAMPLE =
        """
        {"error": "invalid_request", "error_description": "<description of the error>"}""";
    public static final String SERVER_ERROR_EXAMPLE =
        """
        {"error": "server_error", "error_description": "<description of the error>"}""";

    public static final String CSR_RESOURCE_EXAMPLE =
        """
        { "csr": "-----BEGIN NEW CERTIFICATE REQUEST-----\\nMII...\\n...==\\n-----END NEW CERTIFICATE REQUEST-----"}""";
    public static final String CERTIFICATE_RESOURCE_EXAMPLE =
        """
        { "certificate": "-----BEGIN CERTIFICATE-----\\nMII...\\n...==\\n-----END CERTIFICATE-----"}""";
    public static final String CERTIFICATES_RESOURCE_EXAMPLE =
        """
        { "certificates": [""" + CERTIFICATE_RESOURCE_EXAMPLE + "]}";

    public static final String ISSUER_CERTS_ENTITLEMENTS_RESOURCE_EXAMPLE =
        """
        { "entitlements": [{"entitlement": "<entitlement URI>", "certificates": """ +
            CERTIFICATES_RESOURCE_EXAMPLE + "}]}";
}
