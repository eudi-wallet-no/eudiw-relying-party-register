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

    public List<String> selectedEntitlements() {
        return credentialIssuerUrls.stream()
            .filter(ServiceCredentialIssuerUrlForm::isSelected)
            .map(ServiceCredentialIssuerUrlForm::getEntitlement)
            .toList();
    }

    public List<V2RegisterClient.ServiceCredentialIssuerUrl> changedCredentialIssuerUrls() {
        return credentialIssuerUrls.stream()
            .filter(role -> role.isSelected() && role.isHasCredentialIssuerUrl())
            .filter(ServiceCredentialIssuerUrlForm::hasChangedCredentialIssuerUrl)
            .map(ServiceCredentialIssuerUrlForm::toRequest)
            .toList();
    }
}
