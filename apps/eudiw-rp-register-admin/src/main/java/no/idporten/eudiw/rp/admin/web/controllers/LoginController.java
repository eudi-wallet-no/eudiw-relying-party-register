package no.idporten.eudiw.rp.admin.web.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Slf4j
@Controller
public class LoginController {

    @GetMapping("/access-denied")
    public ModelAndView accessDenied() {
        return new ModelAndView("access_denied");
    }
}
