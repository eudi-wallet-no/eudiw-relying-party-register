package no.idporten.eudiw.rp.register.lookup.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class IndexController {

    @ResponseBody
    @GetMapping("/")
    public String index() {
        return "RP register web";
    }

}
