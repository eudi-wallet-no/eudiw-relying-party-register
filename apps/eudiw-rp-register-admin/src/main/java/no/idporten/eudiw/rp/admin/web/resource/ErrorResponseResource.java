package no.idporten.eudiw.rp.admin.web.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ErrorResponseResource(
    @JsonProperty(value = "error", required = true)
    String error,
    @JsonProperty(value = "error_description", required = true)
    String errorDescription
) { }
