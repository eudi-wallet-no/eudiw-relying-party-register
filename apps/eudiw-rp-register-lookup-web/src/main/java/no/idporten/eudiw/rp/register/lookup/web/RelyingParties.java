package no.idporten.eudiw.rp.register.lookup.web;

import no.idporten.eudiw.rp.register.lookup.web.resource.RelyingPartiesResource;
import no.idporten.eudiw.rp.register.lookup.web.resource.RelyingPartyResource;

import java.util.HashMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public record RelyingParties(
    HashMap<UUID, RelyingPartyResource> relyingParties
) {
    public static RelyingParties fromResource(RelyingPartiesResource resource) {
        HashMap<UUID, RelyingPartyResource> asHashMap =
            resource.relyingParties().stream().collect(
                Collectors.toMap(RelyingPartyResource::id,
                                 Function.identity(),
                                 (_, b) -> b,
                                 HashMap::new));
        return new RelyingParties(asHashMap);
    }

    public boolean exists(UUID id) {
        return this.relyingParties.containsKey(id);
    }
    public RelyingPartyResource get(UUID id) {
        return this.relyingParties.get(id);
    }
}
