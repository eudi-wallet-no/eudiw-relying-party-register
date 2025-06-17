package no.idporten.eudiw.rp.admin.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import no.idporten.eudiw.rp.admin.web.form.SearchForm;
import no.idporten.eudiw.rp.admin.web.form.RelyingPartyEditForm;
import no.idporten.eudiw.rp.admin.web.resource.*;
import no.idporten.eudiw.rp.admin.web.resource.accesscertificates.*;
import no.idporten.eudiw.rp.admin.web.search.resultsview.RelyingPartiesView;
import no.idporten.eudiw.rp.admin.web.search.resultsview.RelyingPartyAccessCertificateSummary;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import no.idporten.eudiw.rp.admin.web.form.CreateForm;
import no.idporten.eudiw.rp.admin.service.FormsMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AdminController {

    public static final String searchFormAttrId = "searchFormAttr";

    public static final String detailedViewDataAttrId = "detailedViewDataAttr";
    public static final String certificateSummariesAttrId = "certificateSummariesAttr";

    public static final String csrFormAttrId = "csrFormAttr";
    public static final String newCertificateAttrId = "newCertificateAttr";

    public static final String editFormAttrId = "editFormAttr";

    private final RelyingPartiesService relyingPartiesService;
    private final RelyingPartiesView relyingPartiesView;

    @GetMapping("/search")
    public ModelAndView searchGet(
        @ModelAttribute(searchFormAttrId) @Valid SearchForm searchForm,
        BindingResult bindingResult) {
        ModelAndView mav = new ModelAndView("search_view");

        searchForm = searchForm != null ? searchForm : SearchForm.empty();
        mav.addObject(searchFormAttrId, searchForm);

        if (!bindingResult.hasErrors() && !searchForm.searchTerm().isEmpty()) {
            relyingPartiesView.doSearch(searchForm.toResource());
        }
        return mav;
    }

    @GetMapping("/details")
    public ModelAndView detailsGet(
        @RequestParam("id") @Valid UUID id) {
        RelyingPartyResource relyingPartyResource = relyingPartiesView.get(id);
        List<RelyingPartyAccessCertificateSummary> certificatesResource =
            relyingPartiesService.getCertificatesForRelyingParty(id)
                .toSummaries();
        return new ModelAndView(
            "details_view",
            Map.of(detailedViewDataAttrId, relyingPartyResource,
                   certificateSummariesAttrId, certificatesResource));
    }

    @GetMapping("/registerCsr")
    public ModelAndView registerCsrGet(@RequestParam("id") @Valid UUID id) {
        RelyingPartyResource relyingPartyResource = relyingPartiesView.get(id);
        return new ModelAndView("csr_form_view", Map.of(
            csrFormAttrId, CsrForm.empty(),
            detailedViewDataAttrId, relyingPartyResource));
    }

    @PostMapping("/registerCsr")
    public ModelAndView registerCsrPost(
        @RequestParam("id") @Valid UUID id,
        @ModelAttribute(csrFormAttrId) @Valid CsrForm csrForm,
        BindingResult csrFormBindingResult) {
        RelyingPartyResource relyingPartyResource = relyingPartiesView.get(id);
        ModelAndView mav =
            new ModelAndView("csr_form_view", Map.of(detailedViewDataAttrId, relyingPartyResource));

        // if given CSR is invalid
        if (!csrFormBindingResult.hasErrors()) {
            RelyingPartyCsrResource csrResource =
                new RelyingPartyCsrResource(csrForm.csr());
            RelyingPartyAccessCertificateResource certResource =
                relyingPartiesService.requestCertificateForRelyingParty(id, csrResource);

            mav.setViewName("csr_submit_success_view");
            mav.addObject(newCertificateAttrId, certResource.toSummary());
        }
        return mav;
    }

    @GetMapping("/details/edit")
    public ModelAndView editGet(@RequestParam("id") @Valid UUID id) {
        RelyingPartyResource relyingPartyResource = relyingPartiesView.get(id);
        RelyingPartyEditForm editForm =
            RelyingPartyEditForm.prefillFromRelyingPartyResource(relyingPartyResource);
        return new ModelAndView("edit_form_view", Map.of(
            editFormAttrId, editForm,
            detailedViewDataAttrId, relyingPartyResource
        ));
    }

    @PostMapping("/details/edit")
    public ModelAndView editPost(
        @RequestParam("id") UUID id,
        @ModelAttribute(editFormAttrId) @Valid RelyingPartyEditForm editForm,
        BindingResult editFormBindingResult) {

        RelyingPartyResource relyingPartyResource = relyingPartiesView.get(id);
        ModelAndView mav =
            new ModelAndView("edit_form_view", Map.of(
                detailedViewDataAttrId, relyingPartyResource));

        if (!editFormBindingResult.hasErrors()) {
            EditRelyingPartyResource editResource = editForm.toResource();

            // NOTE: could go to an "are you sure?" page here.

            relyingPartiesView.edit(id, editResource);

            // NOTE: at this point RP is updated, and view returns to details page.
            // could alternatively show a confirmation page.
            mav.setViewName("redirect:/details?id=" + id);
        }

        return mav;
    }

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("createRelyingParty", new CreateForm());
        return "register";
    }

    @PostMapping("/register")
    public String submitRegisterForm(@ModelAttribute CreateForm form) {
        relyingPartiesService.create(FormsMapper.toResource(form));
        return "redirect:/";
    }
}
