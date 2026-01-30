package no.idporten.eudiw.credential.registry.integration.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CredentialMetadata {

    private static final Logger log = LoggerFactory.getLogger(CredentialMetadata.class);
    @JsonProperty("display")
    @Valid
    private List<CredentialMetadataDisplay> display = null;
    @JsonProperty("claims")
    @Valid
    private List<Claims> claims = null;

    public CredentialMetadata(){}


    public void setDisplay(List<CredentialMetadataDisplay> display) {
        this.display = display;
    }

    public List<CredentialMetadataDisplay> getDisplay() {
        return this.display;
    }

    public void setClaims(List<Claims> claims){
        this.claims = claims;
    }

    public List<Claims> getClaims() {
        return this.claims;
    }


}


