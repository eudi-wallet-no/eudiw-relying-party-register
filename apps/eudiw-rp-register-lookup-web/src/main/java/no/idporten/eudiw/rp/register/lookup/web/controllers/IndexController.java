package no.idporten.eudiw.rp.register.lookup.web.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.rp.register.lookup.service.RelyingPartiesService;
import no.idporten.eudiw.rp.register.lookup.service.credentialsservice.CredentialsService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Slf4j
@Controller
@RequiredArgsConstructor
public class IndexController {

    public static final String relyingPartyCountAttrId = "relyingPartyCountAttr";
    public static final String credentialCountAttrId = "credentialCountAttr";

    private final RelyingPartiesService relyingPartiesService;
    private final CredentialsService credentialsService;

    @GetMapping("/")
    public ModelAndView indexGet() {
        ModelAndView mav = new ModelAndView("index_view");
        mav.addObject(relyingPartyCountAttrId, relyingPartyCount());
        mav.addObject(credentialCountAttrId, credentialCount());
        return mav;
    }

    /**
     * Totalt antall brukarstader i registeret. Returnerer null dersom
     * registeret er utilgjengelig, slik at forsida likevel renderes.
     */
    private Long relyingPartyCount() {
        try {
            return relyingPartiesService.count();
        } catch (Exception e) {
            log.warn("Could not fetch relying party count for index page", e);
            return null;
        }
    }

    /**
     * Antall gyldige bevisstypar i sandkassa. Returnerer null dersom
     * bevisregisteret er utilgjengelig, slik at forsida likevel renderes.
     */
    private Long credentialCount() {
        try {
            return credentialsService.count();
        } catch (Exception e) {
            log.warn("Could not fetch credential count for index page", e);
            return null;
        }
    }
}
