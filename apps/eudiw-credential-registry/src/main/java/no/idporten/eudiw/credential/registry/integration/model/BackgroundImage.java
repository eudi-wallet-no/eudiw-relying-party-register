package no.idporten.eudiw.credential.registry.integration.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;

import java.net.URI;

public class BackgroundImage {

    @JsonProperty("uri")
    @Valid
    private URI uri;

    public BackgroundImage(){

    }
    public BackgroundImage(URI uri){
        setUri(uri);
    }

    public void setUri(URI uri){
        this.uri = uri;
    }
    public URI getUri(){
        return this.uri;
    }
}
