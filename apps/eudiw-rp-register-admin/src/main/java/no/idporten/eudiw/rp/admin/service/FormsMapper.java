package no.idporten.eudiw.rp.admin.service;

import no.idporten.eudiw.rp.admin.web.form.CreateForm;
import no.idporten.eudiw.rp.admin.web.form.EAAForm;
import no.idporten.eudiw.rp.admin.web.form.EntitlementForm;
import no.idporten.eudiw.rp.admin.web.resource.CreateRelyingPartyResource;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyEaaResource;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyEntitlementResource;

import java.util.ArrayList;

public class FormsMapper {

    public static CreateRelyingPartyResource toResource(CreateForm createForm) {
        return createForm != null ? new CreateRelyingPartyResource(
            createForm.getOrgnr(),
            createForm.getName(),
            createForm.isPublicSector(),
            createForm.getEntitlements() != null ? createForm.getEntitlements().stream()
                .filter(e -> notBlank(e.getEntitlement()))
                .map(FormsMapper::mapEntitlementForm)
                .toList() : new ArrayList<>(),
            createForm.getEaas() != null ? createForm.getEaas().stream()
                .filter(e -> notBlank(e.getNamespace()) || notBlank(e.getIntent()))
                .map(FormsMapper::mapEAAForm)
                .toList() : new ArrayList<>()) : null;
    }

    private static RelyingPartyEaaResource mapEAAForm(EAAForm eaaForm) {
        return eaaForm != null ? new RelyingPartyEaaResource(
            eaaForm.getNamespace(),
            eaaForm.getIntent()) : null;
    }

    private static RelyingPartyEntitlementResource mapEntitlementForm(EntitlementForm entitlementForm) {
        return entitlementForm != null ? new RelyingPartyEntitlementResource(entitlementForm.getEntitlement()) : null;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
