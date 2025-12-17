package no.idporten.eudiw.rp.admin.web.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyResource;
import no.idporten.eudiw.rp.admin.web.resource.certificates.RelyingPartyCertificateResource;
import no.idporten.eudiw.rp.admin.web.search.resultsview.RelyingPartyCertificateSummary;
import no.idporten.eudiw.rp.admin.web.security.UserAuthorityService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

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

        List<RelyingPartyCertificateSummary> accessCertificates =
            relyingPartyResource.accessCertificates()
                                .stream()
                                .map(RelyingPartyCertificateResource::toSummary)
                                .toList();

        List<RelyingPartyCertificateSummary> issuerCertificates =
            relyingPartyResource.issuerCertificates()
                                .stream()
                                .map(RelyingPartyCertificateResource::toSummary)
                                .toList();

        return new ModelAndView("details_view", Map.of(
            detailedViewDataAttrId, relyingPartyResource,
            certificateSummariesAttrId, accessCertificates,
            issuerSummariesAttrId, issuerCertificates)
        );
    }
}
