package no.idporten.eudiw.rp.admin.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.rp.admin.exception.AdminServiceException;
import no.idporten.eudiw.rp.admin.web.form.RelyingPartyEntitlementFormField;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyEntitlementResource;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public enum RelyingPartyEntitlement {
    SERVICE_PROVIDER   ("Service provider",           "https://uri.etsi.org/19475/Entitlement/Service_Provider"),
    QEAA_PROVIDER      ("Qualified EAA provider",     "https://uri.etsi.org/19475/Entitlement/QEAA_Provider"),
    NON_Q_EAA_PROVIDER ("Non-qualified EAA provider", "https://uri.etsi.org/19475/Entitlement/Non_Q_EAA_Provider"),
    PUB_EAA_PROVIDER   ("Public EAA provider",        "https://uri.etsi.org/19475/Entitlement/PUB_EAA_Provider"),
    PID_PROVIDER       ("PID provider",               "https://uri.etsi.org/19475/Entitlement/PID_Provider");

    private final String desc;
    private final String uri;

    public RelyingPartyEntitlementFormField toFormField() {
        return new RelyingPartyEntitlementFormField(this.uri);
    }
    public RelyingPartyEntitlementResource toResource() {
        return new RelyingPartyEntitlementResource(this);
    }

    private static final Map<String, RelyingPartyEntitlement>
        STRING_TO_ENTITLEMENT_MAP =
        Arrays.stream(RelyingPartyEntitlement.values())
              .collect(Collectors.toMap(RelyingPartyEntitlement::getUri,
                                        Function.identity()));
    public static RelyingPartyEntitlement fromString(String s) {
        if (!STRING_TO_ENTITLEMENT_MAP.containsKey(s)) {
            throw new AdminServiceException(
                "Attempted to create %s from invalid/unrecognized entitlement URI"
                    .formatted(RelyingPartyEntitlement.class.getName()));
        }
        return STRING_TO_ENTITLEMENT_MAP.get(s);
    }
    public static boolean isValidEntitlementString(String s) {
        return STRING_TO_ENTITLEMENT_MAP.containsKey(s);
    }
}
