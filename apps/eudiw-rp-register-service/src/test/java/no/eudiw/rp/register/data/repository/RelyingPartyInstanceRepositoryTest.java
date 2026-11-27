package no.eudiw.rp.register.data.repository;

import no.eudiw.rp.register.domain.BaseEntity;
import no.eudiw.rp.register.domain.WalletRelyingParty;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyEntitlement;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyInstance;
import no.eudiw.rp.register.repository.RegistrationSummaryRow;
import no.eudiw.rp.register.repository.WalletRelyingPartyRepository;
import no.eudiw.rp.register.repository.RelyingPartyInstanceRepository;
import no.eudiw.rp.register.testdata.EntityGenerator;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("When using the RelyingPartyInstanceRepository")
@ActiveProfiles("junit")
public class RelyingPartyInstanceRepositoryTest {

    @Autowired
    private RelyingPartyInstanceRepository repository;

    @Autowired
    private WalletRelyingPartyRepository walletRelyingPartyRepository;

    private static final Random rng = new Random();

    // generates "realistic" data, with 1..4 instances per legal entity,
    // roughly 20% inactive RPs, and 10% synthetic legal entities.
    private static List<WalletRelyingParty> generateRealisticTestData(int n) {
        var walletRelyingParties = EntityGenerator.generateWalletRelyingParties(n);
        List<RelyingPartyInstance> relyingPartyInstances =
            walletRelyingParties.stream().flatMap(le -> EntityGenerator.instances(le).stream()).toList();

        relyingPartyInstances.forEach(rpi -> {
            if (rng.nextFloat() >= 0.8) {
                rpi.setActive(false);
            }
        });
        walletRelyingParties.forEach(le -> {
            if (rng.nextFloat() >= 0.9) {
                String syntheticOrgno =
                    le.getOrgno()
                      .replaceFirst("\\d", rng.nextBoolean() ? "2" : "3");
                le.setOrgno(syntheticOrgno);
            }
        });

        return walletRelyingParties;
    }

    @BeforeAll
    static void initSearchTestData(
        @Autowired WalletRelyingPartyRepository walletRelyingPartyRepository,
        @Autowired RelyingPartyInstanceRepository relyingPartyInstanceRepository) {
        walletRelyingPartyRepository.deleteAll();

        int numTestWalletRelyingParties = 50;
        List<WalletRelyingParty> testWalletRelyingParties = generateRealisticTestData(numTestWalletRelyingParties);
        walletRelyingPartyRepository.saveAllAndFlush(testWalletRelyingParties);

        assertTrue(relyingPartyInstanceRepository.count() >= numTestWalletRelyingParties);
    }

    @Test
    @Transactional
    @DisplayName("Should reject a selected role from another service")
    void shouldRejectASelectedRoleFromAnotherService() {
        var firstInstance = EntityGenerator.generateRelyingParty();
        var otherInstance = EntityGenerator.generateRelyingParty();
        String role = "urn:test:shared";
        EntityGenerator.setRelyingPartyEntitlements(firstInstance, List.of(new RelyingPartyEntitlement(role)));
        EntityGenerator.setRelyingPartyEntitlements(otherInstance, List.of(new RelyingPartyEntitlement(role)));
        repository.saveAndFlush(firstInstance);
        repository.saveAndFlush(otherInstance);

        firstInstance.getRelyingPartyEntitlement(role).orElseThrow().setServiceEntitlement(
            otherInstance.getWalletRelyingPartyService().requireServiceEntitlement(role));

        assertThrows(org.springframework.dao.InvalidDataAccessApiUsageException.class,
            () -> repository.saveAndFlush(firstInstance));
    }

    @Test
    @DisplayName("issuer URL query returns distinct URLs from active registrations only")
    void findActiveCredentialIssuerUrls() {
        String activeIssuerUrl = "https://issuer-" + UUID.randomUUID() + ".example.com";
        String inactiveIssuerUrl = "https://issuer-" + UUID.randomUUID() + ".example.com";
        String inactiveServiceIssuerUrl = "https://issuer-" + UUID.randomUUID() + ".example.com";
        String inactivePartyIssuerUrl = "https://issuer-" + UUID.randomUUID() + ".example.com";
        RelyingPartyInstance activeInstance = EntityGenerator.generateRelyingParty();
        EntityGenerator.setRelyingPartyEntitlements(activeInstance, List.of(
            new RelyingPartyEntitlement("first"),
            new RelyingPartyEntitlement("second")
        ));
        RelyingPartyInstance inactiveInstance = EntityGenerator.generateRelyingParty();
        inactiveInstance.setActive(false);
        EntityGenerator.setRelyingPartyEntitlements(inactiveInstance, List.of(
            new RelyingPartyEntitlement("inactive")
        ));
        RelyingPartyInstance inactiveServiceInstance = EntityGenerator.generateRelyingParty();
        inactiveServiceInstance.getWalletRelyingPartyService().setActive(false);
        EntityGenerator.setRelyingPartyEntitlements(inactiveServiceInstance, List.of(
            new RelyingPartyEntitlement("inactive-service")
        ));
        RelyingPartyInstance inactivePartyInstance = EntityGenerator.generateRelyingParty();
        inactivePartyInstance.getWalletRelyingPartyService().getWalletRelyingParty().revoke();
        EntityGenerator.setRelyingPartyEntitlements(inactivePartyInstance, List.of(
            new RelyingPartyEntitlement("inactive-party")
        ));

        activeInstance.getWalletRelyingPartyService().setCredentialIssuerUrl("first", activeIssuerUrl);
        activeInstance.getWalletRelyingPartyService().setCredentialIssuerUrl("second", activeIssuerUrl);
        repository.saveAndFlush(activeInstance);
        inactiveInstance.getWalletRelyingPartyService().setCredentialIssuerUrl("inactive", inactiveIssuerUrl);
        repository.saveAndFlush(inactiveInstance);
        inactiveServiceInstance.getWalletRelyingPartyService().setCredentialIssuerUrl("inactive-service", inactiveServiceIssuerUrl);
        repository.saveAndFlush(inactiveServiceInstance);
        inactivePartyInstance.getWalletRelyingPartyService().setCredentialIssuerUrl("inactive-party", inactivePartyIssuerUrl);
        repository.saveAndFlush(inactivePartyInstance);

        Set<String> issuerUrls = repository.findActiveCredentialIssuerUrls();

        assertTrue(issuerUrls.contains(activeIssuerUrl));
        assertTrue(Collections.disjoint(
            issuerUrls, Set.of(inactiveIssuerUrl, inactiveServiceIssuerUrl, inactivePartyIssuerUrl)));
    }

    @Test
    @DisplayName("issuer URL query preserves case-sensitive issuer paths")
    void findActiveCredentialIssuerUrls_preservesPathCase() {
        String issuerUrl = "https://issuer-" + UUID.randomUUID() + ".example.com";
        String uppercasePathUrl = issuerUrl + "/Issuer";
        String lowercasePathUrl = issuerUrl + "/issuer";
        RelyingPartyInstance instance = EntityGenerator.generateRelyingParty();
        EntityGenerator.setRelyingPartyEntitlements(instance, List.of(
            new RelyingPartyEntitlement("first"),
            new RelyingPartyEntitlement("second")
        ));
        instance.getWalletRelyingPartyService().setCredentialIssuerUrl("first", uppercasePathUrl);
        instance.getWalletRelyingPartyService().setCredentialIssuerUrl("second", lowercasePathUrl);
        repository.saveAndFlush(instance);

        Set<String> issuerUrls = repository.findActiveCredentialIssuerUrls();

        assertTrue(issuerUrls.containsAll(Set.of(uppercasePathUrl, lowercasePathUrl)));
    }

    @Test
    @DisplayName("issuer URL query excludes null URLs")
    void findActiveCredentialIssuerUrls_excludesNullUrls() {
        RelyingPartyInstance instance = EntityGenerator.generateRelyingParty();
        EntityGenerator.setRelyingPartyEntitlements(instance, List.of(
            new RelyingPartyEntitlement("without-url")
        ));
        instance.getWalletRelyingPartyService().setCredentialIssuerUrl("without-url", null);
        repository.saveAndFlush(instance);

        Set<String> issuerUrls = repository.findActiveCredentialIssuerUrls();

        assertFalse(issuerUrls.contains(null));
    }

    @Test
    @Transactional
    @DisplayName("Retrieve all registrations belonging to the selected organization")
    void shouldRetrieveAllRegistrationsForOrganization() {
        RelyingPartyInstance first = EntityGenerator.generateRelyingPartyInstance("Kontrollteneste", List.of(), List.of(), List.of());
        RelyingPartyInstance second = EntityGenerator.generateRelyingPartyInstance("Innsynsteneste", List.of(), List.of(), List.of());
        walletRelyingPartyRepository.saveAndFlush(new WalletRelyingParty("Testverksemd", "123456785", true,
            List.of(first.getWalletRelyingPartyService(), second.getWalletRelyingPartyService())));
        RelyingPartyInstance otherOrganization = EntityGenerator.generateRelyingPartyInstance("Anna teneste", List.of(), List.of(), List.of());
        walletRelyingPartyRepository.saveAndFlush(new WalletRelyingParty("Anna verksemd", "223456782", true,
            List.of(otherOrganization.getWalletRelyingPartyService())));

        List<RegistrationSummaryRow> result = repository.findRegistrationSummariesByOrgno("123456785");

        assertEquals(Set.of(first.getId(), second.getId()), result.stream()
            .map(RegistrationSummaryRow::instanceId).collect(Collectors.toSet()));
    }

    @Test
    @Transactional
    @DisplayName("Should match the legal name despite a trade name and require every selected role")
    void shouldMatchLegalNameAndRequireEverySelectedRole() {
        String searchTerm = UUID.randomUUID().toString();
        RelyingPartyInstance matchingInstance = EntityGenerator.generateRelyingParty();
        var relyingParty = matchingInstance.getWalletRelyingPartyService().getWalletRelyingParty();
        relyingParty.setName("Legal " + searchTerm);
        relyingParty.setTradeName("Unrelated trade name");
        EntityGenerator.setRelyingPartyEntitlements(matchingInstance, List.of(
            new RelyingPartyEntitlement("urn:first"), new RelyingPartyEntitlement("urn:second")));
        RelyingPartyInstance missingRole = EntityGenerator.generateRelyingParty();
        missingRole.getWalletRelyingPartyService().getWalletRelyingParty().setName("Legal " + searchTerm);
        EntityGenerator.setRelyingPartyEntitlements(missingRole, List.of(new RelyingPartyEntitlement("urn:first")));
        repository.saveAndFlush(matchingInstance);
        repository.saveAndFlush(missingRole);

        var results = repository.searchRelyingPartyInstances(null, null, searchTerm, false, false,
            Set.of("urn:first", "urn:second"), 2, PageRequest.of(0, 25, Sort.by("createdMs", "id")));

        assertEquals(List.of(matchingInstance.getId()), results.getContent().stream().map(BaseEntity::getId).toList());
    }

    @ParameterizedTest
    @ValueSource(strings = {"RelyingPartyInstance", "WalletRelyingPartyService", "WalletRelyingParty"})
    @Transactional
    @DisplayName("Should include an inactive registration hierarchy only when requested")
    void shouldIncludeInactiveHierarchyOnlyWhenRequested(String inactiveResource) {
        String searchTerm = UUID.randomUUID().toString();
        RelyingPartyInstance activeInstance = EntityGenerator.generateRelyingParty();
        activeInstance.getWalletRelyingPartyService().setServiceTradeName("Active " + searchTerm);
        RelyingPartyInstance inactiveInstance = EntityGenerator.generateRelyingParty();
        var relyingPartyService = inactiveInstance.getWalletRelyingPartyService();
        relyingPartyService.setServiceTradeName("Inactive " + searchTerm);
        switch (inactiveResource) {
            case "RelyingPartyInstance" -> inactiveInstance.setActive(false);
            case "WalletRelyingPartyService" -> relyingPartyService.setActive(false);
            case "WalletRelyingParty" -> relyingPartyService.getWalletRelyingParty().revoke();
        }
        repository.saveAndFlush(activeInstance);
        repository.saveAndFlush(inactiveInstance);
        var page = PageRequest.of(0, 25, Sort.by("createdMs", "id"));

        var activeResults = repository.searchRelyingPartyInstances(null, null, searchTerm, false, false, Set.of(), 0, page);
        var allResults = repository.searchRelyingPartyInstances(null, null, searchTerm, true, false, Set.of(), 0, page);

        assertEquals(List.of(activeInstance.getId()), activeResults.getContent().stream().map(BaseEntity::getId).toList());
        assertEquals(Set.of(activeInstance.getId(), inactiveInstance.getId()),
            allResults.getContent().stream().map(BaseEntity::getId).collect(Collectors.toSet()));
    }

    @Test
    @DisplayName("Should page relying-party instances in service-name order")
    void shouldPageRelyingPartyInstancesInServiceNameOrder() {
        String searchTerm = UUID.randomUUID().toString();
        RelyingPartyInstance bravo = EntityGenerator.generateRelyingParty();
        bravo.getWalletRelyingPartyService().setServiceTradeName("bravo " + searchTerm);
        RelyingPartyInstance alpha = EntityGenerator.generateRelyingParty();
        alpha.getWalletRelyingPartyService().setServiceTradeName("alpha " + searchTerm);
        repository.saveAndFlush(bravo);
        repository.saveAndFlush(alpha);

        Sort ordering = Sort.by("walletRelyingPartyService.serviceTradeName", "id");
        UUID firstPageId = repository.searchRelyingPartyInstances(
            null, null, searchTerm, true, false, Set.of(), 0, PageRequest.of(0, 1, ordering)).getContent().getFirst().getId();
        UUID secondPageId = repository.searchRelyingPartyInstances(
            null, null, searchTerm, true, false, Set.of(), 0, PageRequest.of(1, 1, ordering)).getContent().getFirst().getId();

        assertEquals(List.of(alpha.getId(), bravo.getId()), List.of(firstPageId, secondPageId));
    }

    @ParameterizedTest
    @CsvSource({
        "false, false, 200000001;800000001;900000001",
        "false, true, 800000001;900000001",
        "true, false, 200000001;200000002;800000001;800000002;900000001",
        "true, true, 800000001;800000002;900000001"
    })
    @Transactional
    @DisplayName("Should combine synthetic, inactive and role filters before counting and paging registrations")
    void shouldCombineSyntheticInactiveAndRoleFiltersBeforeCountingAndPaging(
        boolean includeInactive, boolean hideSyntheticOrgnos, String expectedOrganizationNumbers) {
        String searchTerm = UUID.randomUUID().toString();

        for (String orgno : List.of("800000001", "900000001", "200000001", "800000002", "200000002", "900000002")) {
            var instance = EntityGenerator.generateRelyingParty();
            var service = instance.getWalletRelyingPartyService();
            service.setServiceTradeName(searchTerm);
            service.getWalletRelyingParty().setOrgno(orgno);
            instance.setActive(!orgno.endsWith("2"));
            EntityGenerator.setRelyingPartyEntitlements(instance, List.of(
                new RelyingPartyEntitlement("urn:first"),
                new RelyingPartyEntitlement("urn:second")));

            if (orgno.equals("900000002")) {
                instance.setActive(true);
                EntityGenerator.setRelyingPartyEntitlements(instance, List.of(new RelyingPartyEntitlement("urn:first")));
            }

            repository.saveAndFlush(instance);
        }

        List<String> expected = List.of(expectedOrganizationNumbers.split(";"));
        List<String> actual = new ArrayList<>();
        Sort ordering = Sort.by("walletRelyingPartyService.walletRelyingParty.orgno", "id");

        for (int page = 0; page < expected.size(); page++) {
            var results = repository.searchRelyingPartyInstances(
                null, null, searchTerm, includeInactive, hideSyntheticOrgnos,
                Set.of("urn:first", "urn:second"), 2, PageRequest.of(page, 1, ordering));
            assertEquals(expected.size(), results.getTotalElements());
            actual.add(results.getContent().getFirst().getWalletRelyingPartyService().getWalletRelyingParty().getOrgno());
        }

        assertEquals(expected, actual);
    }
}
