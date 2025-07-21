package no.idporten.eudiw.rp.admin.web.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import no.idporten.eudiw.rp.admin.web.form.RelyingPartyEditForm;
import no.idporten.eudiw.rp.admin.web.resource.EditRelyingPartyResource;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyResource;
import no.idporten.eudiw.rp.admin.web.search.resultsview.RelyingPartiesView;
import no.idporten.eudiw.rp.admin.web.search.resultsview.RelyingPartyAccessCertificateSummary;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class DetailedViewController {

    public static final String detailedViewDataAttrId = SearchController.detailedViewDataAttrId;
    public static final String certificateSummariesAttrId = "certificateSummariesAttr";
    public static final String editFormAttrId = "editFormAttr";

    private final RelyingPartiesService relyingPartiesService;
    private final RelyingPartiesView relyingPartiesView;

    @GetMapping("/details/{id}")
    public ModelAndView detailsGet(
        @PathVariable("id") @Valid UUID id) {
        RelyingPartyResource relyingPartyResource = relyingPartiesService.get(id);
        List<RelyingPartyAccessCertificateSummary> certificatesResource =
            relyingPartiesService.getCertificatesForRelyingParty(id)
                                 .toSummaries();
        return new ModelAndView(
            "details_view",
            Map.of(detailedViewDataAttrId, relyingPartyResource,
                   certificateSummariesAttrId, certificatesResource));
    }

    @GetMapping("/details/{id}/edit")
    public ModelAndView editGet(
        @PathVariable("id") @Valid UUID id) {
        RelyingPartyResource relyingPartyResource = relyingPartiesService.get(id);
        RelyingPartyEditForm editForm =
            RelyingPartyEditForm.prefillFromRelyingPartyResource(relyingPartyResource);
        return new ModelAndView("edit_form_view", Map.of(
            editFormAttrId, editForm,
            detailedViewDataAttrId, relyingPartyResource
        ));
    }

    @PostMapping("/details/{id}/edit")
    public ModelAndView editPost(
        @PathVariable("id") UUID id,
        @ModelAttribute(editFormAttrId) @Valid RelyingPartyEditForm editForm,
        BindingResult editFormBindingResult) {

        RelyingPartyResource relyingPartyResource = relyingPartiesService.get(id);
        ModelAndView mav =
            new ModelAndView("edit_form_view", Map.of(
                detailedViewDataAttrId, relyingPartyResource));

        if (!editFormBindingResult.hasErrors()) {
            EditRelyingPartyResource editResource = editForm.toResource();

            relyingPartiesView.edit(id, editResource);

            // NOTE: at this point RP is updated, and view returns to details page.
            // could alternatively show a confirmation page.

            mav.setViewName("redirect:/details/" + id);
        }
        return mav;
    }
}
