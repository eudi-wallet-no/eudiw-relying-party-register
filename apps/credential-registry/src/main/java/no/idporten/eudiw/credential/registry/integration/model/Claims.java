package no.idporten.eudiw.credential.registry.integration.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Claims {
    @NotNull
    @JsonProperty("path")
    private List<@NotBlank String> path;
    @JsonProperty("mandatory")
    private boolean mandatory = false;
    @JsonProperty("display")
    private List<Display> display = new ArrayList<>();

    public Claims() {

    }
    public Claims(List<String> path) {
        setPath(path);
    }

    public void setPath(List<String> path){
        this.path = path;
    }

    public List<String> getPath(){
        return this.path;
    }

    public void setMandatory(boolean mandatory){
        this.mandatory = mandatory;
    }

    public boolean getMandatory(){
        return this.mandatory;
    }

    public void setDisplay(List<Display> display){
        this.display = display;
    }

    public List<Display> getDisplay(){
        return this.display;
    }
}
