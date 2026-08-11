package no.idporten.eudiw.rp.admin.web.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Slf4j
@Controller
public class LoginController {

    public static final String AUTHENTICATION_ERROR_CODE_ATTR = "authnErrorCodeAttr";

    @GetMapping("/login")
    public ModelAndView loginGet(@RequestParam(value = "error", required = false) String errorCode) {
        ModelAndView mav = new ModelAndView("login_view");
        if (errorCode != null) {
            mav.addObject(AUTHENTICATION_ERROR_CODE_ATTR, errorCode);
        }
        return mav;
    }
}
