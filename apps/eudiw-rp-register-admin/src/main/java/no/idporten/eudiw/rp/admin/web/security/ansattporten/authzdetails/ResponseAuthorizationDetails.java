package no.idporten.eudiw.rp.admin.web.security.ansattporten.authzdetails;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import no.idporten.validators.orgnr.Orgnr;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ResponseAuthorizationDetails(
    @NotBlank
    @JsonProperty("type")
    String type,
    @NotBlank
    @JsonProperty("resource")
    String resource,
    @NotBlank
    @JsonProperty("resource_name")
    String resourceName,
    @NotNull
    @JsonProperty("reportees")
    List<Reportee> reportees
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Reportee(
        @JsonProperty("Name")
        @NotBlank
        String name,
        @JsonProperty("ID")
        @Orgnr
        String orgno
    ) {
        @ConstructorBinding
        public Reportee(String name, String orgno) {
            this.name = name;
            this.orgno = orgno.replaceFirst("^.*:", "");
        }
    }
}
