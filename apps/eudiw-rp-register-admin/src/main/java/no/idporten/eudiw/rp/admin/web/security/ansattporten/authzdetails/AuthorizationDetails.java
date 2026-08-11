package no.idporten.eudiw.rp.admin.web.security.ansattporten.authzdetails;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import no.idporten.validators.orgnr.Orgnr;

import java.util.List;

public interface AuthorizationDetails {

    @JsonProperty("type")
    @NotBlank
    String getType();


    interface Response extends AuthorizationDetails {

        @NotNull
        List<@Valid AuthorizedParties> getAuthorizedParties();

        boolean canMatchRequest(AuthorizationDetails.Request request);

        @JsonIgnoreProperties(ignoreUnknown = true)
        record AuthorizedParties(
                @JsonProperty(value = "orgno")
                @NotNull @Valid Orgno orgno,
                @JsonProperty(value = "name")
                @NotBlank String name

        ){
            public AuthorizedParties(Orgno orgno, String name) {
                this.orgno = orgno;
                this.name = name;
            }
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        record Orgno(
                @NotNull
                @Orgnr
                @JsonProperty(value = "ID", required = true)
                String id,
                @JsonProperty("authority")
                String authority
        ){
                public Orgno(String id, String authority) {
                    this.id = id.replaceFirst("^.*:", "");
                    this.authority = authority;
                }
        }
    }

    interface Request extends AuthorizationDetails { }
}
