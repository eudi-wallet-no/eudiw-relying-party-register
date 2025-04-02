package no.idporten.eudiw.rp.register.lookup.web.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RelyingPartiesResource(
    @JsonProperty(value = "relying_parties", required = true)
    List<RelyingPartyResource> relyingParties
) {
    public Map<UUID, RelyingPartyResource> toMap() {
        return this.relyingParties
                   .stream()
                   .collect(
                       Collectors.toMap(
                           RelyingPartyResource::id,
                           Function.identity()));
    }
}
