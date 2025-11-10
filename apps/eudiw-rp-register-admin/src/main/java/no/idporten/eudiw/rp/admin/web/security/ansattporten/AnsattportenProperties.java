package no.idporten.eudiw.rp.admin.web.security.ansattporten;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import no.idporten.eudiw.rp.admin.web.security.ansattporten.authzdetails.AuthorizationDetails;
import no.idporten.eudiw.rp.admin.web.security.ansattporten.authzdetails.AuthorizationDetailsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@EqualsAndHashCode
@Validated
@ConfigurationProperties(prefix = "eudiw-admin-web.security.ansattporten")
public class AnsattportenProperties {

    @Autowired
    private AuthorizationDetailsMapper authorizationDetailsMapper;

    @Valid
    @NotNull
    private List<AuthorizationDetails.Request> requestAuthorizationDetails = List.of();

    @Valid
    @NotNull
    private List<AuthorizationDetails.Request> entraIdRequestAuthorizationDetails =
        List.of();

    private boolean allowSyntheticReportee = false;

    private boolean allowEntraId = false;

    public void setRequestAuthorizationDetails(
        List<Map<String, Object>> requestAuthorizationDetails) {
        this.requestAuthorizationDetails =
            authorizationDetailsMapper.asRequests(requestAuthorizationDetails);
    }

    public void setEntraIdRequestAuthorizationDetails(
        List<Map<String, Object>> entraIdRequestAuthorizationDetails) {
        this.entraIdRequestAuthorizationDetails =
            authorizationDetailsMapper.asRequests(entraIdRequestAuthorizationDetails);
    }
}
