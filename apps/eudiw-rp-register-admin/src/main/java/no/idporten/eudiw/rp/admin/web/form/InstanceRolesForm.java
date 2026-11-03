package no.idporten.eudiw.rp.admin.web.form;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;
import no.idporten.eudiw.rp.admin.integrations.registerservice.V2RegisterClient;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyResource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@Setter
public class InstanceRolesForm {

    @Valid
    private List<RelyingPartyEntitlementFormField> entitlements = new ArrayList<>();

    @Valid
    private List<RelyingPartyEaaFormField> eaas = new ArrayList<>();

    public static InstanceRolesForm from(RelyingPartyResource registration, List<InstanceRoleOption> options) {
        Map<String, String> issuerUrls = registration.relyingPartyEntitlements().stream()
            .filter(role -> role.credentialIssuerUrl() != null)
            .collect(Collectors.toMap(
                role -> role.entitlement(), role -> role.credentialIssuerUrl(), (first, second) -> first));
        var existing = registration.relyingPartyEntitlements().stream()
            .map(role -> role.entitlement())
            .collect(Collectors.toSet());

        var form = new InstanceRolesForm();
        options.forEach(option -> form.entitlements.add(new RelyingPartyEntitlementFormField(
            existing.contains(option.entitlement()) ? option.entitlement() : null,
            issuerUrls.get(option.entitlement()),
            option.displayName())));
        registration.relyingPartyEaas().forEach(eaa -> form.eaas.add(RelyingPartyEaaFormField.fromResource(eaa)));
        return form;
    }

    public void addMissingRoleRows(List<InstanceRoleOption> options) {
        while (entitlements.size() < options.size()) {
            entitlements.add(new RelyingPartyEntitlementFormField());
        }
        for (int i = 0; i < options.size(); i++) {
            if (entitlements.get(i) == null) {
                entitlements.set(i, new RelyingPartyEntitlementFormField());
            }
            entitlements.get(i).setDisplayName(options.get(i).displayName());
        }
    }

    public List<RelyingPartyEntitlementFormField> selectedEntitlements() {
        return entitlements.stream()
            .filter(role -> role != null && role.getEntitlement() != null && !role.getEntitlement().isBlank())
            .toList();
    }

    public V2RegisterClient.InstanceRolesRequest toRequest() {
        return new V2RegisterClient.InstanceRolesRequest(
            selectedEntitlements().stream()
                .map(role -> new V2RegisterClient.RoleRequest(role.getEntitlement(), blankToNull(role.getCredentialIssuerUrl())))
                .toList(),
            eaas.stream()
                .filter(eaa -> eaa != null && eaa.isSet() && !eaa.getNamespace().isBlank() && !eaa.getIntent().isBlank())
                .map(RelyingPartyEaaFormField::toResource)
                .toList());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
