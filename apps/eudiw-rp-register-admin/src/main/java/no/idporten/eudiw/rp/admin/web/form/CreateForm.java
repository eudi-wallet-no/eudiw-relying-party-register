package no.idporten.eudiw.rp.admin.web.form;

import lombok.Getter;
import lombok.Setter;
import no.idporten.eudiw.rp.admin.web.resource.CreateRelyingPartyResource;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyEaaResource;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyEntitlementResource;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class CreateForm {

    private String orgnr;

    private String name;

    private boolean publicSector;

    private List<EAAForm> eaas = new ArrayList<>(List.of(new EAAForm()));

    private List<EntitlementForm> entitlements = new ArrayList<>(List.of(new EntitlementForm()));
}