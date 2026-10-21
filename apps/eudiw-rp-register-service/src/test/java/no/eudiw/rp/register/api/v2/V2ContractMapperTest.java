package no.eudiw.rp.register.api.v2;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import no.eudiw.rp.register.api.v2.resource.V2Resources;
import no.eudiw.rp.register.config.RegistryProperties;
import no.eudiw.rp.register.config.SupervisoryAuthorityProperties;
import no.eudiw.rp.register.domain.IntendedUse;
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
    void walletRelyingPartyUpdateReplacesEditableFieldsAndPreservesHierarchyAndIdentity() {
        WalletRelyingParty walletRelyingParty = walletRelyingParty();
        WalletRelyingPartyService walletRelyingPartyService =
            new WalletRelyingPartyService("Service", walletRelyingParty, List.of());
        walletRelyingParty.getServices().add(walletRelyingPartyService);
        walletRelyingParty.setTradeName("Original");
        walletRelyingParty.setEmail(List.of("old@example.test"));
        walletRelyingParty.setActive(false);
        var originalId = walletRelyingParty.getId();
        var originalLegalPerson = walletRelyingParty.getLegalPerson();
        String originalOrganizationNumber = walletRelyingParty.getOrgno();
        var request = new V2Resources.WalletRelyingPartyWriteResource(
            null, false, "NO", new V2Resources.LegalPersonResource(List.of("Updated"), List.of()),
            List.of(new V2Resources.IdentifierResource("EUID", "NO-123")),
            List.of(new V2Resources.PolicyResource("PrivacyPolicy", "https://example.test/policy")), List.of(),
            List.of(), List.of(), List.of(), List.of("https://example.test/info"));

        v2ContractMapper.updateWalletRelyingParty(walletRelyingParty, request);

        assertNull(walletRelyingParty.getTradeName());
        assertEquals(List.of(), walletRelyingParty.getEmail());
        assertEquals(List.of("Updated"), walletRelyingParty.getLegalPerson().getLegalName());
        assertSame(originalLegalPerson, walletRelyingParty.getLegalPerson());
        assertEquals(originalId, walletRelyingParty.getId());
        assertEquals(originalOrganizationNumber, walletRelyingParty.getOrgno());
        assertFalse(walletRelyingParty.isActive());
        assertEquals(List.of(walletRelyingPartyService), walletRelyingParty.getServices());
    }

    @Test
    void walletRelyingPartyServiceUpdateClearsNullableFieldsAndPreservesRelatedResources() {
        WalletRelyingParty walletRelyingParty = walletRelyingParty();
        RelyingPartyInstance relyingPartyInstance = new RelyingPartyInstance(List.of(), List.of(), List.of());
        WalletRelyingPartyService walletRelyingPartyService = new WalletRelyingPartyService(
            "Original", walletRelyingParty, List.of(relyingPartyInstance));
        walletRelyingPartyService.setServiceIdentifier("old-identifier");
        walletRelyingPartyService.setSupportUri("https://example.test/old-support");
        walletRelyingPartyService.setActive(false);
        IntendedUse intendedUse = mock(IntendedUse.class);
        walletRelyingPartyService.getIntendedUses().add(intendedUse);
        var request = new V2Resources.WalletRelyingPartyServiceWriteResource(
            "Updated", null, null, "contact@example.test", null,
            List.of(List.of(new V2Resources.MultiLangStringResource("en", "Updated description"))),
            List.of(), List.of(), List.of(), false, List.of());
        var originalId = walletRelyingPartyService.getId();

        v2ContractMapper.updateWalletRelyingPartyService(walletRelyingPartyService, request);

        assertEquals("Updated", walletRelyingPartyService.getServiceTradeName());
        assertNull(walletRelyingPartyService.getServiceIdentifier());
        assertNull(walletRelyingPartyService.getSupportUri());
        assertEquals(originalId, walletRelyingPartyService.getId());
        assertSame(walletRelyingParty, walletRelyingPartyService.getWalletRelyingParty());
        assertEquals(List.of(relyingPartyInstance), walletRelyingPartyService.getRelyingPartyInstances());
        assertEquals(List.of(intendedUse), walletRelyingPartyService.getIntendedUses());
        assertFalse(walletRelyingPartyService.isActive());
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
    }

    @Test
    void instanceUpdateClearsCollectionsWithoutChangingParentOrStatus() {
        RelyingPartyInstance relyingPartyInstance = new RelyingPartyInstance(
            List.of(new RelyingPartyEntitlement("WRP", null)), List.of(), List.of());
        WalletRelyingPartyService walletRelyingPartyService =
            new WalletRelyingPartyService("Service", walletRelyingParty(), List.of(relyingPartyInstance));
        relyingPartyInstance.setActive(false);

        var request = new V2Resources.RelyingPartyInstanceWriteResource(List.of(), List.of());
        v2ContractMapper.updateRelyingPartyInstance(
            relyingPartyInstance, request, v2ContractMapper.toDomainRelyingPartyEntitlements(request.relyingPartyEntitlements()));

        assertTrue(relyingPartyInstance.getRelyingPartyEntitlements().isEmpty());
        assertSame(walletRelyingPartyService, relyingPartyInstance.getWalletRelyingPartyService());
        assertFalse(relyingPartyInstance.isActive());
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
    void explicitNullForRequiredBooleanProducesValidationError() {
        var request = new V2Resources.WalletRelyingPartyServiceWriteResource(
            "Service", null, null, "contact@example.test", null,
            List.of(List.of(new V2Resources.MultiLangStringResource("en", "Description"))),
            List.of(), List.of(), List.of(), null, List.of());

        assertThrows(BadRequestException.class, () -> v2ContractMapper.toWalletRelyingPartyService(
            request, walletRelyingParty(), List.of()));
    }

    @Test
    void walletRelyingPartyReadUsesExplicitTs5FieldNamesAndConfiguredPublicRegistryUri() {
        var resource = v2ContractMapper.toWalletRelyingPartyResource(walletRelyingParty());
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
