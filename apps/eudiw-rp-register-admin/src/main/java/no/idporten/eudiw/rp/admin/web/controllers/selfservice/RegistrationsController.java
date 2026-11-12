package no.idporten.eudiw.rp.admin.web.controllers.selfservice;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.rp.admin.service.V2RegistrationsService;
import no.idporten.eudiw.rp.admin.web.form.RegistrationSearchForm;
import no.idporten.eudiw.rp.admin.web.resource.RegistrationSummary;
import no.idporten.eudiw.rp.admin.web.security.UserAuthorityService;
import no.idporten.eudiw.rp.admin.web.security.oidcusers.AuthorizedPartyAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class RegistrationsController {
    public static final String registrationsAttrId = "registrationsAttr";
    private final V2RegistrationsService registrationsService;
    private final UserAuthorityService userAuthorityService;

    @GetMapping("/registrations")
    public ModelAndView registrationsGet(
        @ModelAttribute("searchFormAttr") @Valid RegistrationSearchForm searchForm,
        BindingResult bindingResult) {

        AuthorizedPartyAuthority reportee = userAuthorityService.getAuthorizedPartyAuthority();
        List<RegistrationSummary> registrations = bindingResult.hasErrors() ? List.of()
            : registrationsService.searchByOrgno(reportee.orgno(), searchForm.getSearchTerm());
        ModelAndView mav = new ModelAndView("registrations_view", "searchFormAttr", searchForm);
        mav.addObject(registrationsAttrId, registrations);
        return mav;
    }
}
