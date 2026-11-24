package no.idporten.eudiw.rp.admin.web.form;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import no.idporten.eudiw.rp.admin.validation.SaneStringConstraint;
import no.idporten.eudiw.rp.admin.validation.SaneStringValidator;

@Getter
@Setter
@NoArgsConstructor
public class ServiceSummaryForm {

    @SaneStringConstraint(message =
        "Ugyldig tenestenamn. Gyldige teikn er: norske bokstavar, tal, mellomrom og symbola "
            + SaneStringValidator.ALLOWED_SYMBOLS)
    @NotBlank(message = "Tenestenamn må fyllast ut")
    private String serviceTradeName = "";

    public ServiceSummaryForm(String serviceTradeName) {
        this.serviceTradeName = serviceTradeName;
    }
}
