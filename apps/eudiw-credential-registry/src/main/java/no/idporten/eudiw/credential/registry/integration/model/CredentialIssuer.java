package no.idporten.eudiw.credential.registry.integration.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


//TODO: Write in support for credential_request_encryption and credential_response_encryption according to spec openid4vci
@Validated
@JsonIgnoreProperties(ignoreUnknown = true)
public class CredentialIssuer {

    @JsonProperty("credential_issuer")
    @NotBlank
    private String credentialIssuer;
    @JsonProperty("authorization_servers")
    private List<String> authorizationServers = null;
    @JsonProperty("credential_endpoint")
    @Valid
    private URL credentialEndpoint;
    @JsonProperty("nonce_endpoint")
    private String nonceEndpoint = "";
    @JsonProperty("deferred_credential_endpoint")
    private String deferredCredentialEndpoint = "";
    @JsonProperty("notification_endpoint")
    private String notificationEndpoint = "";
    @JsonProperty("batch_credential_issuance")
    private BatchCredentialIssuance batchCredentialIssuance = new BatchCredentialIssuance();
    @JsonProperty("display")
    private List<@Valid Display> display = null;
    @JsonProperty("credential_configurations_supported")
    @NotNull
    private Map<String, @Valid CredentialConfiguration> credentialConfiguration;

    public CredentialIssuer(){

    }

    public CredentialIssuer(String credentialIssuer, URL credentialEndpoint, Map<String, CredentialConfiguration> credentialConfiguration){
        setCredentialIssuer(credentialIssuer);
        setCredentialEndpoint(credentialEndpoint);
        setCredentialConfiguration(credentialConfiguration);
    }

    public void setCredentialIssuer(String credentialIssuer){
        this.credentialIssuer = credentialIssuer;
    }
    public String getCredentialIssuer(){
        return this.credentialIssuer;
    }
    public void setAuthorizationServers(List<String> authorizationServers){
        this.authorizationServers = authorizationServers;
    }

    public List<String> getAuthorizationServers(){
        return this.authorizationServers;
    }

    public void setCredentialEndpoint(URL credentialEndpoint){
        this.credentialEndpoint = credentialEndpoint;
    }
    public URL getCredentialEndpoint(){
        return this.credentialEndpoint;
    }
    public void setNonceEndpoint(String nonceEndpoint){
            this.nonceEndpoint = nonceEndpoint;
    }
    public String getNonceEndpoint(){
        return this.nonceEndpoint;
    }
    public void setDeferredCredentialEndpoint(String deferredCredentialEndpoint){
        this.deferredCredentialEndpoint = deferredCredentialEndpoint;
    }
    public String getDeferredCredentialEndpoint(){
        return this.deferredCredentialEndpoint;
    }
    public void setNotificationEndpoint(String notificationEndpoint) {
        this.notificationEndpoint = notificationEndpoint;
    }
    public String getNotificationEndpoint(){
        return this.notificationEndpoint;
    }

    public void setBatchCredentialIssuance(BatchCredentialIssuance batchCredentialIssuance){
        this.batchCredentialIssuance = batchCredentialIssuance;
    }

    public BatchCredentialIssuance getBatchCredentialIssuance(Integer batchCredentialIssuance){
        return this.batchCredentialIssuance;
    }

    public void setDisplay(List<Display> display){
        this.display = display;
    }

    public List<Display> getDisplay(){
        return this.display;
    }

    public void setCredentialConfiguration(Map<String, CredentialConfiguration> credentialConfiguration) {
        this.credentialConfiguration = credentialConfiguration;
    }

    public Map<String, CredentialConfiguration> getCredentialConfiguration(){
        return this.credentialConfiguration;
    }
}





