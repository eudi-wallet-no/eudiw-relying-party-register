package no.eudiw.rp.register.api.v2.resource;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record SupervisoryAuthorityResource(
    String name,
    String country,
    List<String> phone,
    List<String> email,
    @JsonProperty("formURI") List<String> formUri
) { }
