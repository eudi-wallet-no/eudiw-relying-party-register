package no.idporten.eudiw.ca.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import no.idporten.validators.orgnr.Orgnr;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Schema(description = "Request for certificate", title = "Certificate issue request", type = "object")
public class CertificateRequest {

    @Schema(description = "Organization number.  Will be included in certificate subject DN",
            example = "991825827")
    @Orgnr(message = "Invalid organization number")
    @NotEmpty
    @JsonProperty("orgno")
    private final String orgno;

    @Schema(description = "Legal name.  Will be included in certificate subject DN",
            example = "Digitaliseringsdirektoratet")
    @NotEmpty(message = "Empty legal name")
    @JsonProperty("legal_name")
    private final String legalName;

    @Schema(description = "Trade name.  Will be included in certificate subject DN",
            example = "Stjerneporten")
    @NotEmpty(message = "Empty trade name")
    @JsonProperty("trade_name")
    private final String tradeName;

    @Schema(description = "Certificate Signing Request (PEM-encoded CSR - line breaks must be escaped)",
            example = """
            -----BEGIN NEW CERTIFICATE REQUEST-----
            MII...
            -----END NEW CERTIFICATE REQUEST-----
            """)
    @NotEmpty(message = "Empty csr")
    @JsonProperty("csr")
    private final String csr;

}
