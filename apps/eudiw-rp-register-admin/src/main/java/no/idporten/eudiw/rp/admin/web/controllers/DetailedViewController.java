package no.idporten.eudiw.rp.admin.web.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import no.idporten.eudiw.rp.admin.service.credentialsservice.CredentialsService;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyEntitlementResource;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyResource;
import no.idporten.eudiw.rp.admin.web.search.resultsview.RelyingPartyCertificateSummary;
import no.idporten.eudiw.rp.admin.web.search.resultsview.RelyingPartyCertificateSummaryBuilder;
import no.idporten.eudiw.rp.admin.web.security.UserAuthorityService;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequiredArgsConstructor
public class DetailedViewController {

    public static final String DETAILED_VIEW_DATA_ATTR = SearchController.DETAILED_VIEW_DATA_ATTR;
    public static final String ACCESS_CERTIFICATE_SUMMARIES_ATTR = "accessCertificateSummariesAttr";
    public static final String ISSUER_CERTIFICATE_SUMMARIES_ATTR = "issuerCertificateSummariesAttr";

    public static final String CREDENTIAL_ERRORS_ATTR = "credentialErrorsAttr";

    private final RelyingPartiesService relyingPartiesService;
    private final CredentialsService credentialsService;
    private final UserAuthorityService userAuthorityService;
    private final RelyingPartyCertificateSummaryBuilder relyingPartyCertificateSummaryBuilder;

    @GetMapping("/details")
    public ModelAndView detailsWithoutIdRedirectToSearch() {
        return new ModelAndView("redirect:/search");
    }

    @GetMapping("/details/{id}")
    public ModelAndView detailsGet(@PathVariable("id") @Valid UUID id) {
        RelyingPartyResource relyingPartyResource = relyingPartiesService.get(id);
        userAuthorityService.assertUserHasAccessTo(relyingPartyResource.orgno());

        var credentialErrors = relyingPartyResource.getIssuerEntitlements().stream()
                .map(RelyingPartyEntitlementResource::credentialIssuerUrl)
                .filter(StringUtils::hasText).distinct()
                .collect(Collectors.toMap(Function.identity(), credentialsService::getCredentialErrors));

        List<RelyingPartyCertificateSummary> accessCertificates =
            relyingPartyResource.accessCertificates()
                                .stream()
                                .map(relyingPartyCertificateSummaryBuilder::build)
                                .toList();


        List<RelyingPartyCertificateSummary> issuerCertificates =
            relyingPartyResource.issuerCertificates()
                                .stream()
                                .map(relyingPartyCertificateSummaryBuilder::build)
                                .toList();

        return new ModelAndView("details_view", Map.of(
            DETAILED_VIEW_DATA_ATTR, relyingPartyResource,
            ACCESS_CERTIFICATE_SUMMARIES_ATTR, accessCertificates,
            ISSUER_CERTIFICATE_SUMMARIES_ATTR, issuerCertificates,
            CREDENTIAL_ERRORS_ATTR, credentialErrors)
        );
    }
}
