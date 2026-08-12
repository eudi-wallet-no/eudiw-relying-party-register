package no.idporten.eudiw.ca.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.bouncycastle.asn1.x509.CRLReason;
import org.hibernate.validator.constraints.Range;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Schema(description = "Request to revoke certificate", title = "Certificate revoke request", type = "object")
public class RevokeCertificateRequest {

    @Schema(description = "Serial number",
            example = "1234567890")
    @NotEmpty
    @JsonProperty("serial_number")
    private String serialNumber;

    @Schema(description = "Revocation reason",
            example = "" + CRLReason.keyCompromise)
    @Range(min = CRLReason.unspecified, max = CRLReason.aACompromise, message = "Invalid revocation reason.")
    @JsonProperty("reason")
    private int reason = CRLReason.keyCompromise;

}
