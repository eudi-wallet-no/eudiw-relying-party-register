package no.idporten.eudiw.rp.admin.web.controllers;

import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.rp.admin.web.security.UserAuthorityService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequiredArgsConstructor
public class IndexController {

    private final UserAuthorityService userAuthorityService;

    @GetMapping("/")
    public ModelAndView index() {
        if (!userAuthorityService.userHasAdminAuthority()) {
            return new ModelAndView("redirect:/registrations");
        }
        return new ModelAndView("redirect:/search");
    }
}
