package no.idporten.eudiw.credential.registry.integration.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;

import java.net.URI;

public class Logo{
    @JsonProperty("uri")
    @Valid
    private URI uri;
    @JsonProperty("alt_text")
    private String altText = "";

    public Logo() {

    }

    public Logo(URI uri) {
        setUri(uri);
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }
    public URI getUri() {
        return this.uri;
    }
    public void setAltText(String altText) {
            this.altText = altText;
    }

    public String getAltText() {
        return this.altText;
    }
}
