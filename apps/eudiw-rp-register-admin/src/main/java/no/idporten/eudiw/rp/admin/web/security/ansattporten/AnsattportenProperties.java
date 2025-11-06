package no.idporten.eudiw.rp.admin.web.security.ansattporten;

import jakarta.validation.Valid;
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
    private List<AuthorizationDetails.Request> requestAuthorizationDetails = List.of();

    private boolean allowSyntheticReportee = false;

    public void setRequestAuthorizationDetails(List<Map<String, Object>> requestAuthorizationDetails) {
        this.requestAuthorizationDetails =
            requestAuthorizationDetails.stream()
                                       .map(authorizationDetailsMapper::asRequest)
                                       .toList();
    }
}
