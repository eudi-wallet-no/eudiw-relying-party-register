package no.eudiw.rp.register.api.resource;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record ErrorResponseResource(
    @NotNull
    String error,
    @NotNull
    @JsonProperty("error_description")
    String errorDescription
) { }
