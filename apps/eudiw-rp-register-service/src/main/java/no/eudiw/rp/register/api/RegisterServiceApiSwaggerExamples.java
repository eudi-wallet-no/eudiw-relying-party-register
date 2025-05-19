package no.eudiw.rp.register.api;

public class RegisterServiceApiSwaggerExamples {
    private RegisterServiceApiSwaggerExamples() { }

    protected static final String NOT_FOUND_ERROR_EXAMPLE =
        "{\"error\": \"not_found\", \"error_description\": \"<description of the error>\"}";
    protected static final String BAD_REQUEST_ERROR_EXAMPLE =
        "{\"error\": \"invalid_request\", \"error_description\": \"<description of the error>\"}";
    protected static final String SERVER_ERROR_EXAMPLE =
        "{\"error\": \"server_error\", \"error_description\": \"<description of the error>\"}";

    protected static final String CSR_RESOURCE_EXAMPLE =
        "{ \"csr\": \"-----BEGIN NEW CERTIFICATE REQUEST-----\\n" +
            "MII...\\n...==\\n-----END NEW CERTIFICATE REQUEST-----\"}";
    protected static final String CERTIFICATE_RESOURCE_EXAMPLE =
        "{ \"certificate\": \"-----BEGIN CERTIFICATE-----\\n" +
            "MII...\\n...==\\n-----END CERTIFICATE-----\"}";
    protected static final String CERTIFICATES_RESOURCE_EXAMPLE =
        "{ \"certificates\": [" + CERTIFICATE_RESOURCE_EXAMPLE + "]}";
}
