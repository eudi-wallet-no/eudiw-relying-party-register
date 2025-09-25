package no.idporten.eudiw.rp.admin.web.security.ansattporten;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import no.idporten.validators.orgnr.Orgnr;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AnsattportenReportee(
    @JsonProperty("Name")
    @NotBlank
    String name,
    // TODO: according to Jørgen, the "ID" field can be assumed to be a Norwegian orgno,
    // but the "Authority" field can be used to determine how to parse the "ID" field
    // in general.
    @JsonProperty("ID")
    @Orgnr
    String orgno
    // @JsonProperty("Authority")
    // String authority
) {
    @ConstructorBinding
    public AnsattportenReportee(String name, String orgno) {
        this.name = name;
        this.orgno = orgno.replaceFirst("^.*:", "");
    }
}
