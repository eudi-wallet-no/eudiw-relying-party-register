package no.eudiw.rp.register.api.v2.endpoints.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import no.eudiw.rp.register.api.v2.resource.IdentifierResource;
import no.eudiw.rp.register.api.v2.resource.LegalPersonResource;
import no.eudiw.rp.register.api.v2.resource.PolicyResource;
import no.eudiw.rp.register.api.v2.resource.V2ResourceValidation;
import no.eudiw.rp.register.domain.WalletRelyingParty;

import java.util.ArrayList;
import java.util.List;

public record RelyingPartyRequest(
    String tradeName,
    @JsonProperty(required = true) @NotNull Boolean isPSB,
    @JsonProperty(required = true) @NotBlank String country,
    @Valid LegalPersonResource legalPerson,
    @JsonProperty(required = true) @NotEmpty List<@NotNull @Valid IdentifierResource> identifier,
    @JsonProperty(required = true) @NotEmpty List<@NotNull @Valid PolicyResource> policy,
    List<@NotBlank String> x5c,
    List<@NotBlank String> postalAddress,
    List<@NotBlank String> email,
    List<@NotBlank String> phone,
    List<@NotBlank String> infoURI
) {
    @AssertTrue(message = "invalid_info_uri")
    public boolean hasValidInfoUris() {
        return infoURI == null || infoURI.stream().allMatch(V2ResourceValidation::isAbsoluteUri);
    }

    public void update(WalletRelyingParty relyingParty) {
        relyingParty.setTradeName(tradeName);
        relyingParty.setPSB(isPSB);
        relyingParty.setCountry(country);

        if (legalPerson == null) {
            relyingParty.setLegalPerson(null);
        } else {
            var domainLegalPerson = legalPerson.toDomain();
            if (relyingParty.getLegalPerson() == null) {
                relyingParty.setLegalPerson(domainLegalPerson);
            } else {
                relyingParty.getLegalPerson().setLegalName(domainLegalPerson.getLegalName());
                relyingParty.getLegalPerson().setEstablishedByLaw(domainLegalPerson.getEstablishedByLaw());
            }
        }

        relyingParty.setIdentifier(identifier.stream().map(item -> item.toDomain()).toList());
        relyingParty.setPolicy(policy.stream().map(item -> item.toDomain()).toList());
        relyingParty.setX5c(listOrEmpty(x5c));
        relyingParty.setPostalAddress(listOrEmpty(postalAddress));
        relyingParty.setEmail(listOrEmpty(email));
        relyingParty.setPhone(listOrEmpty(phone));
        relyingParty.setInfoURI(listOrEmpty(infoURI));
        relyingParty.setUpdated();
    }

    private static <T> List<T> listOrEmpty(List<T> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }
}
