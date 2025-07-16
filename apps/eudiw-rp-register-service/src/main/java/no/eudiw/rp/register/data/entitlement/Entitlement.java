package no.eudiw.rp.register.data.entitlement;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import no.eudiw.rp.register.api.resource.RelyingPartyEntitlementResource;
import no.eudiw.rp.register.exception.RegisterServiceException;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public enum Entitlement {
    SERVICE_PROVIDER   ("Service provider",           "https://uri.etsi.org/19475/Entitlement/Service_Provider"),
    QEAA_PROVIDER      ("Qualified EAA provider",     "https://uri.etsi.org/19475/Entitlement/QEAA_Provider"),
    NON_Q_EAA_PROVIDER ("Non-qualified EAA provider", "https://uri.etsi.org/19475/Entitlement/Non_Q_EAA_Provider"),
    PUB_EAA_PROVIDER   ("Public EAA provider",        "https://uri.etsi.org/19475/Entitlement/PUB_EAA_Provider"),
    PID_PROVIDER       ("PID provider",               "https://uri.etsi.org/19475/Entitlement/PID_Provider");

    private final String desc;
    private final String value;
    public RelyingPartyEntitlementResource toResource() {
        return new RelyingPartyEntitlementResource(this);
    }

    private static final Map<String, Entitlement>
        STRING_TO_ENTITLEMENT_MAP =
        Arrays.stream(Entitlement.values()).collect(
            Collectors.toMap(Entitlement::getValue,
                             Function.identity()));

    public static Entitlement fromString(String s) {
        if (!STRING_TO_ENTITLEMENT_MAP.containsKey(s)) {
            throw new RegisterServiceException(
                "Attempted to internalize invalid entitlement string");
        }
        return STRING_TO_ENTITLEMENT_MAP.get(s);
    }
}
