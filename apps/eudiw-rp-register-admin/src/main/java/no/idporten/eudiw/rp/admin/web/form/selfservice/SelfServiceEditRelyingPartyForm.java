package no.idporten.eudiw.rp.admin.web.form.selfservice;

import jakarta.validation.Valid;
import lombok.*;
import no.idporten.eudiw.rp.admin.web.form.RelyingPartyEaaFormField;

import java.util.List;

@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class SelfServiceEditRelyingPartyForm {
    @Valid
    private List<RelyingPartyEaaFormField> eaas;
}
