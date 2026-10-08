package no.idporten.eudiw.credential.registry.integration.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Claims {
    @NotEmpty
    @JsonProperty("path")
    private List<Object> path;
    @JsonProperty("mandatory")
    private boolean mandatory = false;
    @JsonProperty("display")
    private List<Display> display = new ArrayList<>();

    public Claims() {

    }
    public Claims(List<Object> path) {
        setPath(path);
    }

    public void setPath(List<Object> path){
        this.path = path;
    }

    public List<Object> getPath(){
        return this.path;
    }

    // OpenID4VCI Appendix C: strings, null (all array elements), or non-negative integer indices.
    @JsonIgnore
    @AssertTrue(message = "claim path must contain only strings, null, or non-negative integers")
    public boolean isValidPath() {
        return path == null || path.stream().allMatch(element ->
                element == null || element instanceof String
                        || ((element instanceof Byte || element instanceof Short
                             || element instanceof Integer || element instanceof Long
                             || element instanceof BigInteger)
                            && new BigInteger(element.toString()).signum() >= 0));
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
