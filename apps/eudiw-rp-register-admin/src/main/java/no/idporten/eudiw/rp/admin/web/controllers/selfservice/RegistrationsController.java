package no.idporten.eudiw.rp.admin.web.controllers.selfservice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyResource;
import no.idporten.eudiw.rp.admin.web.security.UserAuthorityService;
import no.idporten.eudiw.rp.admin.web.security.oidcusers.AuthorizedPartyAuthority;
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
    private final UserAuthorityService userAuthorityService;

    @GetMapping("/registrations")
    public ModelAndView registrationsGet() {
        AuthorizedPartyAuthority reportee = userAuthorityService.getAuthorizedPartyAuthority();
        List<RelyingPartyResource> registrations =
            relyingPartiesService.getAllByOrgno(reportee.orgno()).relyingParties();
        return new ModelAndView("registrations_view", registrationsAttrId, registrations);
    }
}
