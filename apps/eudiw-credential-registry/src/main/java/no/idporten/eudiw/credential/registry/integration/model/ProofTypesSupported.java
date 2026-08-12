package no.idporten.eudiw.credential.registry.integration.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown=true)
public class ProofTypesSupported{
    @JsonProperty("jwt")
    ProofSigningAlgValuesSupported jwt;

    public ProofTypesSupported(){

    }

    public ProofTypesSupported(ProofSigningAlgValuesSupported jwt) {
        this.jwt = jwt;
    }
}
