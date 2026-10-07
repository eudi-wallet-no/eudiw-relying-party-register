package no.eudiw.rp.register.api.v1.resource.relyingparty;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.With;
import no.eudiw.rp.register.api.v1.resource.certificates.RelyingPartyCertificateResource;
import no.eudiw.rp.register.validation.SaneStringConstraint;
import no.idporten.validators.orgnr.Orgnr;

import java.util.List;
import java.util.UUID;

@With
@JsonInclude(JsonInclude.Include.ALWAYS)
public record RelyingPartyResource(

    @NotNull(message = "null_id")
    @JsonProperty("id")
    UUID id,

    @Orgnr
    @NotNull(message = "null_orgno")
    @JsonProperty(value = "org_nr", required = true)
    String orgNr,

    @SaneStringConstraint
    @NotBlank(message = "blank_name")
    @JsonProperty(value = "org_name", required = true)
    String orgName,

    @SaneStringConstraint
    @NotBlank(message = "blank_name")
    @JsonProperty(value = "name", required = true)
    String tradeName,

    @JsonProperty(value = "public_sector", required = true)
    boolean publicSector,

    @Valid
    @NotEmpty(message = "empty_entitlements")
    @JsonProperty("relying_party_entitlements")
    List<RelyingPartyEntitlementResource> relyingPartyEntitlements,

    @Valid
    @NotNull(message = "null_eaas")
    @JsonProperty("relying_party_eaas")
    List<RelyingPartyEaaResource> relyingPartyEaas,

    @Valid
    @NotNull(message = "null_access_certificates")
    @JsonProperty("access_certificates")
    List<RelyingPartyCertificateResource> accessCertificates,

    @Valid
    @NotNull(message = "null_issuer_certificates")
    @JsonProperty("issuer_certificates")
    List<RelyingPartyCertificateResource> issuerCertificates,

    @JsonProperty(value = "created_ms", required = false)
    long createdMs,

    @JsonProperty(value = "last_updated_ms", required = false)
    long lastUpdatedMs,

    @JsonProperty(value = "active", required = true)
    boolean active
) { }
