package no.eudiw.rp.register.api.v2;

import no.eudiw.rp.register.api.v2.endpoints.entities.RegistrationDetailsResponse;
import no.eudiw.rp.register.api.v2.endpoints.entities.RegistrationDetailsResponse.RelyingPartyEntitlementResponse;
import no.eudiw.rp.register.domain.Entitlement;
import no.eudiw.rp.register.domain.WalletRelyingParty;
import no.eudiw.rp.register.domain.WalletRelyingPartyService;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyEntitlement;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RegistrationDetailsResponseTest {

    private RelyingPartyInstance relyingPartyInstance;

    @BeforeEach
    void setUp() {
        relyingPartyInstance = new RelyingPartyInstance(List.of(), List.of(), List.of());
        var relyingPartyService = new WalletRelyingPartyService("Service name", null, List.of(relyingPartyInstance));
        new WalletRelyingParty("Party name", "123456785", true, List.of(relyingPartyService));
    }

    @Test
    @DisplayName("Should receive every registered role in order, including repeats and names from inactive catalog entries")
    void shouldReturnEveryRegisteredRoleInOrderWithCatalogNames() {
        relyingPartyInstance.getRelyingPartyEntitlements().addAll(List.of(
            new RelyingPartyEntitlement("urn:issuer", "https://issuer.test"),
            new RelyingPartyEntitlement("urn:reader", null),
            new RelyingPartyEntitlement("urn:issuer", "https://other-issuer.test")));
        Map<String, Entitlement> entitlementCatalog = Map.of(
            "urn:issuer", new Entitlement("urn:issuer", false, "Issuer", "test-ca"),
            "urn:reader", new Entitlement("urn:reader", true, "Reader", "test-ca"));

        var registrationDetails = RegistrationDetailsResponse.from(relyingPartyInstance, entitlementCatalog);

        assertEquals(List.of(new RelyingPartyEntitlementResponse("urn:issuer", "Issuer", "https://issuer.test"),
            new RelyingPartyEntitlementResponse("urn:reader", "Reader", null),
            new RelyingPartyEntitlementResponse("urn:issuer", "Issuer", "https://other-issuer.test")),
            registrationDetails.relyingPartyEntitlements());
    }

    @Test
    @DisplayName("Should identify a registered role by its URI when the role is absent from the catalog")
    void shouldIdentifyUncataloguedRoleByItsRegisteredUri() {
        relyingPartyInstance.getRelyingPartyEntitlements().add(new RelyingPartyEntitlement("urn:unknown", "https://issuer.test"));

        assertEquals(List.of(new RelyingPartyEntitlementResponse("urn:unknown", "urn:unknown", "https://issuer.test")),
            RegistrationDetailsResponse.from(relyingPartyInstance, Map.of()).relyingPartyEntitlements());
    }
}
