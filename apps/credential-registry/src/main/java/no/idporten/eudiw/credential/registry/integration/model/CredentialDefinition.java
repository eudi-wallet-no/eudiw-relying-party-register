package no.idporten.eudiw.credential.registry.integration.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Objects;

public class CredentialDefinition {

    @JsonProperty("type")
    @NotNull
    @Size(min = 2, message = "Expected at least two type values")
    private List<@NotBlank String> type;

    public CredentialDefinition() {
    }

    @JsonIgnore
    @AssertTrue(message = "First item of credential_definition.type must be \"VerifiableCredential\"")
    public boolean isValidTypeList() {
        return type != null && Objects.equals(type.getFirst(), "VerifiableCredential");
    }

    public void setType(List<String> type) {
        this.type = type;
    }

    public List<String> getType() {
        return this.type;
    }
}
