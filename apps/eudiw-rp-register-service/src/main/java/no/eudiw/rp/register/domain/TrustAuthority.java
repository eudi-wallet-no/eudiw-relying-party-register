package no.eudiw.rp.register.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TrustAuthority(String frameworkType, String value, @JsonProperty("isLOTE") Boolean isLOTE) {
}
