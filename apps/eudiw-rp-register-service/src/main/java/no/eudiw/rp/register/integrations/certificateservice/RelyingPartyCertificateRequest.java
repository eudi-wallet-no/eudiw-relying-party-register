package no.eudiw.rp.register.integrations.certificateservice;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RelyingPartyCertificateRequest {

    @JsonProperty("orgno")
    private String orgno;
    @JsonProperty("trade_name")
    private String tradeName;
    @JsonProperty("legal_name")
    private String legalName;
    @JsonProperty("csr")
    private String csr;

}
