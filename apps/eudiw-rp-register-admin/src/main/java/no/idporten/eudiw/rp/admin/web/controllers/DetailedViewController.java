package no.idporten.eudiw.rp.admin.web.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyEntitlementResource;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyResource;
import no.idporten.eudiw.rp.admin.web.search.resultsview.RelyingPartyAccessCertificateSummary;
import no.idporten.eudiw.rp.admin.web.search.resultsview.RelyingPartyIssuerCertificateSummary;
import no.idporten.eudiw.rp.admin.web.security.UserAuthorityService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class DetailedViewController {

    public static final String detailedViewDataAttrId = SearchController.detailedViewDataAttrId;
    public static final String certificateSummariesAttrId = "certificateSummariesAttr";
    public static final String issuerSummariesAttrId = "issuerSummariesAttr";

    private final RelyingPartiesService relyingPartiesService;
    private final UserAuthorityService userAuthorityService;

    @GetMapping("/details")
    public ModelAndView detailsWithoutIdRedirectToSearch() {
        return new ModelAndView("redirect:/search");
    }

    @GetMapping("/details/{id}")
    public ModelAndView detailsGet(@PathVariable("id") @Valid UUID id) {
        RelyingPartyResource relyingPartyResource = relyingPartiesService.get(id);
        userAuthorityService.assertUserHasAccessTo(relyingPartyResource.orgno());

        List<RelyingPartyAccessCertificateSummary> certificatesResource =
            relyingPartiesService.getCertificatesForRelyingParty(id)
                                 .toSummaries();

        List<RelyingPartyEntitlementResource> entitlements = relyingPartiesService.getIssuerCertificateForRelyingParty(id).entitlements();
        List<RelyingPartyIssuerCertificateSummary> issuerCerts = new ArrayList<>();
        for (RelyingPartyEntitlementResource entitlement : entitlements) {
            issuerCerts.addAll(entitlement.toIssuerCertificateSummaries());
        }

        return new ModelAndView("details_view", Map.of(
            detailedViewDataAttrId, relyingPartyResource,
            certificateSummariesAttrId, certificatesResource,
            issuerSummariesAttrId, issuerCerts)
        );
    }
}
