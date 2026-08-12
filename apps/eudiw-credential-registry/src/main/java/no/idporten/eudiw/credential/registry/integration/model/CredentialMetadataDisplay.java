package no.idporten.eudiw.credential.registry.integration.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.net.URI;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CredentialMetadataDisplay extends Display {
    @JsonProperty("name")
    @NotNull
    private String name;

    @JsonProperty("description")
    private String description = "";

    @JsonProperty("background_color")
    private String backgroundColor = "";

    @JsonProperty("background_image")
    private BackgroundImage backgroundImage =  new BackgroundImage(URI.create(""));

    @JsonProperty("text_color")
    private String textColor = "";

    public CredentialMetadataDisplay() {

    }

    public CredentialMetadataDisplay(String name) {
        super(name);
    }

    public void setName(String name) {
        this.name = name;
    }
    public String getName() {
        return name;
    }

    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }
    public String getBackgroundColor() {
        return backgroundColor;
    }
    public void setBackgroundImage(BackgroundImage backgroundImage) {
        this.backgroundImage = backgroundImage;
    }
    public BackgroundImage getBackgroundImage() {
        return backgroundImage;
    }


    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return this.description;
    }

    public void setTextColor(String textColor) {
        this.textColor = textColor;
    }

    public String getTextColor() {
        return this.textColor;
    }

}
