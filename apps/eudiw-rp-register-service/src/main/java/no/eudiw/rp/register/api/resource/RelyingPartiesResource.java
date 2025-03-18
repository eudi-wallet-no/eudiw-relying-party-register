package no.eudiw.rp.register.api.resource;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RelyingPartiesResource {

    @JsonProperty("relying_parties")
    private List<RelyingPartyResource> relyingParties;
}
