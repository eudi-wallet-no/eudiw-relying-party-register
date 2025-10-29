package no.idporten.eudiw.rp.admin.web.security.ansattporten;

import jakarta.validation.Valid;
import no.idporten.eudiw.rp.admin.web.security.ansattporten.authzdetails.RequestAuthorizationDetails;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "eudiw-admin-web.security.ansattporten")
public record AnsattportenProperties(
    @Valid
    RequestAuthorizationDetails authorizationDetails,
    boolean allowSyntheticReportee
) { }
