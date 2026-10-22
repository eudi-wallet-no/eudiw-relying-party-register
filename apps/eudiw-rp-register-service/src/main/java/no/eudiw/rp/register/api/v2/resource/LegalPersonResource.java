package no.eudiw.rp.register.api.v2.resource;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record LegalPersonResource(
    @JsonProperty(required = true) @NotEmpty List<@NotBlank String> legalName,
    List<@NotNull @Valid LawResource> establishedByLaw
) { }
