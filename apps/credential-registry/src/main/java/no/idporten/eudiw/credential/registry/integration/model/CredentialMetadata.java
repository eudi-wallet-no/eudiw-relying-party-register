package no.idporten.eudiw.credential.registry.integration.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CredentialMetadata {

    private static final Logger log = LoggerFactory.getLogger(CredentialMetadata.class);
    @JsonProperty("display")
    private List<DisplayWithRequiredName> display = new ArrayList<>();
    @JsonProperty("claims")
    private List<Claims> claims = new ArrayList<>();

    public CredentialMetadata(){}


    public void setDisplay(List<DisplayWithRequiredName> display) {
        this.display = display;
    }

    public List<DisplayWithRequiredName> getDisplay() {
        return this.display;
    }

    public void setClaims(List<Claims> claims){
        this.claims = claims;
    }

    public List<Claims> getClaims() {
        return this.claims;
    }


}


