package no.idporten.eudiw.rp.admin.web.form;

import jakarta.validation.constraints.NotBlank;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import no.idporten.eudiw.rp.admin.integrations.registerservice.V2RegisterClient;
import no.idporten.eudiw.rp.admin.validation.SaneStringConstraint;
import no.idporten.eudiw.rp.admin.validation.SaneStringValidator;

import java.util.List;

@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class BaseCreateRelyingPartyForm {

    @SaneStringConstraint(message =
        "Ugyldig tenestenamn. Gyldige teikn er: norske bokstavar, tal, mellemrom og symbola "
            + SaneStringValidator.ALLOWED_SYMBOLS)
    @NotBlank(message = "Tenestenamn må fyllast ut")
    private String tradeName = "";

    public static final List<V2RegisterClient.RoleRequest> DEFAULT_NON_ADMIN_ENTITLEMENTS =
        List.of(new V2RegisterClient.RoleRequest(
            "https://uri.etsi.org/19475/Entitlement/Service_Provider"));

    public V2RegisterClient.CreateInstanceRequest toRequest(String orgno) {
        return new V2RegisterClient.CreateInstanceRequest(
            orgno,
            this.tradeName,
            DEFAULT_NON_ADMIN_ENTITLEMENTS
        );
    }
}
