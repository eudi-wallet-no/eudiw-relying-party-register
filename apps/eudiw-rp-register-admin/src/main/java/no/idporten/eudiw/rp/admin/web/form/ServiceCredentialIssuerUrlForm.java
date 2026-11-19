package no.idporten.eudiw.rp.admin.web.form;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import no.idporten.eudiw.rp.admin.integrations.registerservice.V2RegisterClient;

import java.net.URI;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
public class ServiceCredentialIssuerUrlForm {

    private String entitlement;
    private String credentialIssuerUrl;
    private String originalCredentialIssuerUrl;
    private String displayName;
    private boolean serviceRole;
    private boolean hasCredentialIssuerUrl;
    private boolean selected;

    public boolean hasChangedCredentialIssuerUrl() {
        return !Objects.equals(credentialIssuerUrlOrNull(credentialIssuerUrl),
            credentialIssuerUrlOrNull(originalCredentialIssuerUrl));
    }

    public boolean hasValidCredentialIssuerUrl() {
        if (credentialIssuerUrl == null || credentialIssuerUrl.isBlank()) {
            return true;
        }

        try {
            var uri = URI.create(credentialIssuerUrl);

            return "https".equals(uri.getScheme()) && uri.getHost() != null;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public V2RegisterClient.ServiceCredentialIssuerUrl toRequest() {
        return new V2RegisterClient.ServiceCredentialIssuerUrl(entitlement, credentialIssuerUrlOrNull(credentialIssuerUrl));
    }

    private String credentialIssuerUrlOrNull(String url) {
        return url == null || url.isBlank() ? null : url;
    }
}
