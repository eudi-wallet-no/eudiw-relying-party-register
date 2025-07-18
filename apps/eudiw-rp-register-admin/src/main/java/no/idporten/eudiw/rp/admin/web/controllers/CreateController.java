package no.idporten.eudiw.rp.admin.web.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import no.idporten.eudiw.rp.admin.web.form.RelyingPartyCreateForm;
import no.idporten.eudiw.rp.admin.web.resource.CreateRelyingPartyResource;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyResource;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class CreateController {

    public static final String createFormAttrId = "createFormAttr";

    private final RelyingPartiesService relyingPartiesService;

    @GetMapping("/create")
    public ModelAndView createGet() {
        RelyingPartyCreateForm createForm = new RelyingPartyCreateForm();
        return new ModelAndView("create_form_view", Map.of(
            createFormAttrId, createForm
        ));
    }

    @PostMapping("/create")
    public ModelAndView createPost(
        @ModelAttribute(createFormAttrId) @Valid RelyingPartyCreateForm createForm,
        BindingResult createFormBindingResult) {

        ModelAndView mav =
            new ModelAndView("create_form_view", Map.of(
                createFormAttrId, createForm));

        if (!createFormBindingResult.hasErrors()) {
            CreateRelyingPartyResource createResource = createForm.toResource();
            RelyingPartyResource result = relyingPartiesService.create(createResource);
            mav.setViewName("redirect:/details?id=" + result.id());
        }
        return mav;
    }
}
