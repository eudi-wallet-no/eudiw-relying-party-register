package no.idporten.eudiw.rp.admin.web.form;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegistrationStatusForm {

    @NotNull
    private Boolean active;
}
