package no.idporten.eudiw.rp.register.lookup.web.resource.credentials;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CredentialsResource(
    @NotNull(message = "invalid_credentials")
    @JsonProperty(value = "credentials", required = true)
    List<@NotNull @Valid CredentialResource> credentials
) {

    private static final Function<String, Comparator<CredentialResource>> byCredentialTypeDisplayName =
        locale -> Comparator.comparing(
            credential -> credential.getCredentialTypeDisplayName(locale));

    private static final Function<String, Comparator<CredentialResource>> byIssuer =
        locale -> Comparator.comparing(
            credential -> credential.getIssuerDisplayName(locale));

    private static final Comparator<CredentialResource> byNumClaims =
        Comparator.comparing(credential -> credential.getMetadata().getClaims().size());

    private static final Comparator<CredentialResource> byFormat =
        Comparator.comparing(CredentialResource::getFormat);

    public List<CredentialResource> sortBy(String key, String locale) {

        Comparator<CredentialResource> defaultComparator =
            byCredentialTypeDisplayName.apply(locale);

        Comparator<CredentialResource> comparator = switch (key) {
            case "issuer" -> byIssuer.apply(locale).thenComparing(defaultComparator);
            case "num_claims" -> byNumClaims.thenComparing(defaultComparator);
            case "format" -> byFormat.thenComparing(defaultComparator);
            default -> defaultComparator;
        };

        return this.credentials.stream().sorted(comparator).toList();
    }
}
