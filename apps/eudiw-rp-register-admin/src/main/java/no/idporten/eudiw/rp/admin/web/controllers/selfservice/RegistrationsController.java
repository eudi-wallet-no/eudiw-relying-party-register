package no.idporten.eudiw.rp.admin.web.controllers.selfservice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyResource;
import no.idporten.eudiw.rp.admin.web.security.oidcusers.BaseOidcUser;
import no.idporten.eudiw.rp.admin.web.security.oidcusers.SelfServiceOidcUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class RegistrationsController {
    public static final String registrationsAttrId = "registrationsAttr";
    private final RelyingPartiesService relyingPartiesService;

    @GetMapping("/registrations")
    public ModelAndView registrationsGet(@AuthenticationPrincipal BaseOidcUser oidcUser) {
        List<RelyingPartyResource> registrations =
            oidcUser instanceof SelfServiceOidcUser selfServiceUser
                ? selfServiceUser.getReportees()
                   .stream()
                   .map(reportee -> relyingPartiesService.getAllByOrgno(reportee.orgno()))
                   .flatMap(rpsResource -> rpsResource.relyingParties().stream())
                   .toList()
               : List.of();
        return new ModelAndView("registrations_view", registrationsAttrId, registrations);
    }
}
