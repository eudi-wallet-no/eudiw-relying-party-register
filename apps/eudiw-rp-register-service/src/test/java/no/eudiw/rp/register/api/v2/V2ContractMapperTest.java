package no.eudiw.rp.register.api.v2;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import no.eudiw.rp.register.api.v2.resource.V2Resources;
import no.eudiw.rp.register.config.RegistryProperties;
import no.eudiw.rp.register.config.SupervisoryAuthorityProperties;
import no.eudiw.rp.register.domain.WalletRelyingParty;
import no.eudiw.rp.register.domain.WalletRelyingPartyService;
import no.eudiw.rp.register.domain.certificates.IssuerCertificate;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyEntitlement;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyInstance;
import no.eudiw.rp.register.exception.BadRequestException;
import no.eudiw.rp.register.testdata.TestDataGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class V2ContractMapperTest {

    private ValidatorFactory validatorFactory;
    private V2ContractMapper v2ContractMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        v2ContractMapper = new V2ContractMapper(
            validatorFactory.getValidator(),
            new RegistryProperties("https://registry.example.test/wrp"),
            new SupervisoryAuthorityProperties("Authority", "NO", null, null, null)
        );
    }

    @AfterEach
    void tearDown() {
        validatorFactory.close();
    }

    @Test
    void invalidReplacementIsRejectedBeforeMutation() {
        WalletRelyingParty walletRelyingParty = walletRelyingParty();
        var request = new V2Resources.WalletRelyingPartyWriteResource(
            "Changed", true, "NO", new V2Resources.LegalPersonResource(List.of("Name"), List.of()),
            List.of(new V2Resources.IdentifierResource("EUID", "NO-123")),
            List.of(new V2Resources.PolicyResource("PrivacyPolicy", "https://example.test/policy")), List.of(),
            List.of(), List.of(), List.of(), List.of("not a URI"));

        assertThrows(BadRequestException.class,
            () -> v2ContractMapper.updateWalletRelyingParty(walletRelyingParty, request));

        assertEquals("Original", walletRelyingParty.getTradeName());
        assertEquals(0, walletRelyingParty.getLastUpdatedMs());
    }

    @Test
    void instanceUpdateCannotRemoveEntitlementsWithIssuerCertificates() {
        RelyingPartyEntitlement relyingPartyEntitlement = new RelyingPartyEntitlement("WRP", null);
        relyingPartyEntitlement.getIssuerCertificates().add(mock(IssuerCertificate.class));
        RelyingPartyInstance relyingPartyInstance =
            new RelyingPartyInstance(List.of(relyingPartyEntitlement), List.of(), List.of());

        var request = new V2Resources.RelyingPartyInstanceWriteResource(List.of(), List.of());
        var requestedEntitlements = v2ContractMapper.toDomainRelyingPartyEntitlements(
            request.relyingPartyEntitlements());

        assertThrows(BadRequestException.class, () ->
            v2ContractMapper.updateRelyingPartyInstance(relyingPartyInstance, request, requestedEntitlements));

        assertSame(relyingPartyEntitlement, relyingPartyInstance.getRelyingPartyEntitlements().getFirst());
        assertEquals(0, relyingPartyInstance.getLastUpdatedMs());
    }

    @Test
    void walletRelyingPartyUpdateMarksEntityUpdated() {
        WalletRelyingParty walletRelyingParty = walletRelyingParty();
        var request = new V2Resources.WalletRelyingPartyWriteResource(
            "Changed", true, "NO", new V2Resources.LegalPersonResource(List.of("Name"), List.of()),
            List.of(new V2Resources.IdentifierResource("EUID", "NO-123")),
            List.of(new V2Resources.PolicyResource("PrivacyPolicy", "https://example.test/policy")),
            List.of(), List.of(), List.of(), List.of(), List.of());

        v2ContractMapper.updateWalletRelyingParty(walletRelyingParty, request);

        assertTrue(walletRelyingParty.getLastUpdatedMs() > 0);
    }

    @Test
    void walletRelyingPartyServiceUpdateMarksEntityUpdated() {
        var service = new WalletRelyingPartyService("Original", walletRelyingParty(), List.of());
        var request = new V2Resources.WalletRelyingPartyServiceWriteResource(
            "Changed", null, null, "contact@example.test", null,
            List.of(List.of(new V2Resources.MultiLangStringResource("en", "Description"))),
            List.of(), List.of(), List.of(), false, List.of());

        v2ContractMapper.updateWalletRelyingPartyService(service, request);

        assertTrue(service.getLastUpdatedMs() > 0);
    }

    @Test
    void relyingPartyInstanceUpdateMarksEntityUpdated() {
        var instance = new RelyingPartyInstance(List.of(), List.of(), List.of());
        var request = new V2Resources.RelyingPartyInstanceWriteResource(List.of(), List.of());

        v2ContractMapper.updateRelyingPartyInstance(instance, request, List.of());

        assertTrue(instance.getLastUpdatedMs() > 0);
    }

    @Test
    void requiredFieldsAreExplicitWhileOptionalTs5FieldsMayBeOmitted() {
        var party = new V2Resources.WalletRelyingPartyWriteResource(
            null, true, "NO", new V2Resources.LegalPersonResource(List.of("Name"), List.of()),
            List.of(new V2Resources.IdentifierResource("EUID", "NO-123")),
            List.of(new V2Resources.PolicyResource("PrivacyPolicy", "https://example.test/policy")),
            List.of(), List.of(), List.of(), List.of(), List.of());
        assertPropertiesRequired(party, List.of("isPSB", "country", "identifier", "policy"));
        assertPropertiesOptional(party,
            List.of("tradeName", "legalPerson", "x5c", "postalAddress", "email", "phone", "infoURI"));

        var service = new V2Resources.WalletRelyingPartyServiceWriteResource(
            "Service", null, null, "contact@example.test", null,
            List.of(List.of(new V2Resources.MultiLangStringResource("en", "Description"))),
            List.of(), List.of(), List.of(), false, List.of());
        assertPropertiesRequired(service, List.of("serviceTradeName", "srvDescription", "isIntermediary"));
        assertPropertiesOptional(service,
            List.of("serviceIdentifier", "supportURI", "email", "phone", "subEntitlements",
                "providesAttestations", "usesIntermediaries", "servedWRPServices"));
        assertPropertiesRequired(
            new V2Resources.RelyingPartyInstanceWriteResource(List.of(), List.of()),
            List.of("relyingPartyEntitlements", "relyingPartyEaas"));
    }

    @Test
    void walletRelyingPartyReadUsesExplicitTs5FieldNamesAndConfiguredPublicRegistryUri() {
        var resource = v2ContractMapper.toWalletRelyingPartyResponse(walletRelyingParty());
        var json = objectMapper.readTree(objectMapper.writeValueAsString(resource));

        assertTrue(json.get("isPSB").booleanValue());
        assertNotNull(json.get("infoURI"));
        assertEquals("https://registry.example.test/wrp", json.get("registryURI").asString());
        assertEquals("NO", json.get("supervisoryAuthority").get("country").asString());
    }

    private <T> void assertPropertiesRequired(T resource, List<String> propertyNames) {
        ObjectNode complete = (ObjectNode) objectMapper.valueToTree(resource);
        for (String propertyName : propertyNames) {
            ObjectNode incomplete = complete.deepCopy();
            incomplete.remove(propertyName);
            assertThrows(JacksonException.class,
                () -> objectMapper.treeToValue(incomplete, resource.getClass()), propertyName);
        }
    }

    private <T> void assertPropertiesOptional(T resource, List<String> propertyNames) {
        ObjectNode complete = (ObjectNode) objectMapper.valueToTree(resource);
        for (String propertyName : propertyNames) {
            ObjectNode incomplete = complete.deepCopy();
            incomplete.remove(propertyName);
            assertDoesNotThrow(
                () -> objectMapper.treeToValue(incomplete, resource.getClass()), propertyName);
        }
    }

    private WalletRelyingParty walletRelyingParty() {
        return new WalletRelyingParty("Original", TestDataGenerator.generateValidOrgno(), true, List.of());
    }
}
