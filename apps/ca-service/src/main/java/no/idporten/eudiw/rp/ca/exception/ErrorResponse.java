package no.idporten.eudiw.rp.ca.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record ErrorResponse(
    @NotNull
    @JsonProperty("error")
    String error,
    @NotNull
    @JsonProperty("error_description")
    String errorDescription
) { }
