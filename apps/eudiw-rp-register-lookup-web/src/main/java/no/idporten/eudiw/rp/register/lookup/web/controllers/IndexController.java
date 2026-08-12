package no.idporten.eudiw.rp.register.lookup.web.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class IndexController {
    @GetMapping("/")
    public ModelAndView indexGet() {
        return new ModelAndView("index_view");
    }
}
