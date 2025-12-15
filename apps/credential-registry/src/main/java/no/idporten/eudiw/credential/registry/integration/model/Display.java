package no.idporten.eudiw.credential.registry.integration.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;


@JsonIgnoreProperties(ignoreUnknown = true)
public class Display {
    @JsonProperty("name")
    private String name;
    @JsonProperty("locale")
    private String locale = "";
    @JsonProperty("logo")
    private Logo logo = new Logo(URI.create(""));
    @JsonProperty("description") // This parameter is added as "non exhaustive" parameter. We want it, therefore it is
    private String description = ""; // here. It is not explicitly a part of the protocol, but there is room for it.


    public Display() {

    }


    public Display(String name) {
        setName(name);
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getLocale() {
        return this.locale;
    }

    public void setLogo(Logo logo){
        this.logo = logo;
    }

    public Logo getLogo() {
        return this.logo;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return this.description;
    }

}
