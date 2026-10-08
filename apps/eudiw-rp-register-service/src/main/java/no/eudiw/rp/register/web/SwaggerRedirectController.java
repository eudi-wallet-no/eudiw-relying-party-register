package no.eudiw.rp.register.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SwaggerRedirectController {

    @GetMapping("/")
    public String index() {
        return "redirect:/swagger-ui.html";
    }

}
