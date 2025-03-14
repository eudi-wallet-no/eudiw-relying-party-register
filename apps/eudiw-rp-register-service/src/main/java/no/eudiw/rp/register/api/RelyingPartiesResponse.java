package no.eudiw.rp.register.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RelyingPartiesResponse {

    @JsonProperty("relying_parties")
    private List<RelyingPartyResponse> relyingParties;
}
