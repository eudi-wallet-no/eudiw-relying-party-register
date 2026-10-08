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
public class RevocationRequest {
    @JsonProperty("serial_number")
    private String serialNumber;
    @JsonProperty("reason")
    private int reason;
}
