package no.idporten.eudiw.rp.admin.web.form;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;
import no.idporten.eudiw.rp.admin.integrations.registerservice.V2RegisterClient;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyResource;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class InstanceEaasForm {

    @Valid
    private List<RelyingPartyEaaFormField> eaas = new ArrayList<>();

    public static InstanceEaasForm from(RelyingPartyResource registration) {
        var form = new InstanceEaasForm();
        registration.relyingPartyEaas().forEach(eaa -> form.eaas.add(RelyingPartyEaaFormField.fromResource(eaa)));

        return form;
    }

    public V2RegisterClient.InstanceEaasRequest toRequest() {
        return new V2RegisterClient.InstanceEaasRequest(eaas.stream()
            .filter(eaa -> eaa != null && eaa.isSet() && !eaa.getNamespace().isBlank() && !eaa.getIntent().isBlank())
            .map(RelyingPartyEaaFormField::toResource)
            .toList());
    }
}
