package no.idporten.eudiw.rp.register.lookup.web.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.rp.register.lookup.service.RelyingPartiesService;
import no.idporten.eudiw.rp.register.lookup.web.resource.RelyingPartyResource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;


import java.util.Map;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class DetailedViewController {

    public static final String detailedViewDataAttrId = "detailedViewDataAttr";

    private final RelyingPartiesService relyingPartiesService;

    @GetMapping("/details")
    public ModelAndView detailsWithoutIdRedirectToSearch() {
        return new ModelAndView("redirect:/search");
    }

    @GetMapping("/details/{id}")
    public ModelAndView detailsGet(
        @PathVariable("id") @Valid UUID id) {
        RelyingPartyResource relyingPartyResource = relyingPartiesService.get(id);

        return new ModelAndView("details_view", Map.of(
            detailedViewDataAttrId, relyingPartyResource));
    }
}
