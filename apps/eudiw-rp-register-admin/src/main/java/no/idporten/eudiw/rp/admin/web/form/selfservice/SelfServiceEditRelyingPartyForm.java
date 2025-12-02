package no.idporten.eudiw.rp.admin.web.form.selfservice;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import no.idporten.eudiw.rp.admin.validation.SaneStringConstraint;
import no.idporten.eudiw.rp.admin.validation.SaneStringValidator;
import no.idporten.eudiw.rp.admin.web.form.RelyingPartyEaaFormField;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class SelfServiceEditRelyingPartyForm {

    @SaneStringConstraint(message =
        "Ugyldig tenestenamn. Gyldige teikn er: norske bokstavar, tal, mellemrom og symbola "
            + SaneStringValidator.ALLOWED_SYMBOLS)
    @NotBlank(message = "Tenestenamn må fyllast ut")
    private String tradeName = "";

    @Valid
    private List<RelyingPartyEaaFormField> eaas = new ArrayList<>();
}
