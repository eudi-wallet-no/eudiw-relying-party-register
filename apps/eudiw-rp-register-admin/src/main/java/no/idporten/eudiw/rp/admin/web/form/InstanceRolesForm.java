package no.idporten.eudiw.rp.admin.web.form;

import lombok.Getter;
import lombok.Setter;
import no.idporten.eudiw.rp.admin.integrations.registerservice.V2RegisterClient;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyResource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class InstanceRolesForm {

    private List<InstanceRoleFormField> entitlements = new ArrayList<>();

    public static InstanceRolesForm from(RelyingPartyResource registration, List<InstanceRoleOption> options) {
        var existing = registration.relyingPartyEntitlements().stream()
            .map(role -> role.entitlement())
            .collect(Collectors.toSet());

        var form = new InstanceRolesForm();
        options.forEach(option -> form.entitlements.add(new InstanceRoleFormField(
            existing.contains(option.entitlement()) ? option.entitlement() : null,
            option.displayName())));

        return form;
    }

    public void addMissingRoleRows(List<InstanceRoleOption> options) {
        while (entitlements.size() < options.size()) {
            entitlements.add(new InstanceRoleFormField());
        }

        for (int i = 0; i < options.size(); i++) {
            if (entitlements.get(i) == null) {
                entitlements.set(i, new InstanceRoleFormField());
            }

            entitlements.get(i).setDisplayName(options.get(i).displayName());
        }
    }

    public List<InstanceRoleFormField> selectedEntitlements() {
        return entitlements.stream()
            .filter(role -> role != null && role.getEntitlement() != null && !role.getEntitlement().isBlank())
            .toList();
    }

    public V2RegisterClient.InstanceRolesRequest toRequest() {
        return new V2RegisterClient.InstanceRolesRequest(
            selectedEntitlements().stream()
                .map(role -> new V2RegisterClient.RoleRequest(role.getEntitlement()))
                .toList());
    }

}
