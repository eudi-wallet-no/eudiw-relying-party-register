package no.idporten.eudiw.rp.admin.web.form;

import lombok.Getter;
import lombok.Setter;
import no.idporten.eudiw.rp.admin.integrations.registerservice.V2RegisterClient;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ServiceCredentialIssuerUrlsForm {

    private List<ServiceCredentialIssuerUrlForm> credentialIssuerUrls = new ArrayList<>();

    public static ServiceCredentialIssuerUrlsForm from(List<V2RegisterClient.ServiceCredentialIssuerUrl> credentialIssuerUrls) {
        var form = new ServiceCredentialIssuerUrlsForm();
        credentialIssuerUrls.forEach(url -> form.credentialIssuerUrls.add(new ServiceCredentialIssuerUrlForm(
            url.entitlement(), url.credentialIssuerUrl(), url.credentialIssuerUrl())));

        return form;
    }

    public List<V2RegisterClient.ServiceCredentialIssuerUrl> toRequest() {
        return credentialIssuerUrls.stream()
            .filter(ServiceCredentialIssuerUrlForm::hasChangedCredentialIssuerUrl)
            .map(ServiceCredentialIssuerUrlForm::toRequest)
            .toList();
    }
}
