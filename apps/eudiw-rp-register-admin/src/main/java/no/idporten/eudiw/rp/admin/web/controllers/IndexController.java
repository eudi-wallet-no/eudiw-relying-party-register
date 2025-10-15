package no.idporten.eudiw.rp.admin.web.controllers;

import no.idporten.eudiw.rp.admin.web.security.oidcusers.BaseOidcUser;
import no.idporten.eudiw.rp.admin.web.security.oidcusers.SelfServiceOidcUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class IndexController {

    @GetMapping("/")
    public ModelAndView index(@AuthenticationPrincipal BaseOidcUser oidcUser) {
        if (oidcUser instanceof SelfServiceOidcUser) {
            return new ModelAndView("redirect:/registrations");
        }
        return new ModelAndView("index_view");
    }
}
