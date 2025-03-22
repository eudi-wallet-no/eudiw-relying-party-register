package no.eudiw.rp.register.api.resource;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import no.eudiw.rp.register.validation.SaneStringConstraint;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record RelyingPartyEaaResource(

    @SaneStringConstraint
    @NotBlank(message = "blank_namespace")
    @JsonProperty("namespace")
    String namespace,

    @SaneStringConstraint
    @NotBlank(message = "blank_intent")
    @JsonProperty("intent")
    String intent
) { }
