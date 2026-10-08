package no.eudiw.rp.register.service;

import no.eudiw.rp.register.domain.WalletRelyingParty;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyInstance;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyEntitlement;
import no.eudiw.rp.register.repository.AccessCertificateRepository;
import no.eudiw.rp.register.repository.IssuerCertificateRepository;
import no.eudiw.rp.register.repository.RelyingPartyInstanceRepository;
import no.eudiw.rp.register.repository.WalletRelyingPartyRepository;
import no.eudiw.rp.register.repository.WalletRelyingPartyServiceRepository;
import no.eudiw.rp.register.domain.certificates.IssuerCertificate;
import no.eudiw.rp.register.testdata.CertificatesGenerator;
import no.eudiw.rp.register.testdata.EntityGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@ActiveProfiles("junit")
class WalletRelyingPartyLifecycleTest {

    @Autowired
    private RelyingPartyService service;

    @Autowired
    private WalletRelyingPartyRepository parties;

    @Autowired
    private WalletRelyingPartyServiceRepository services;

    @MockitoSpyBean
    private RelyingPartyInstanceRepository instances;

    @Autowired
    private AccessCertificateRepository accessCertificates;

    @Autowired
    private IssuerCertificateRepository issuerCertificates;

    @Test
    void failedInstanceSaveRollsBackNewPartyAndService() {
        long partyCount = parties.count();
        long serviceCount = services.count();
        long instanceCount = instances.count();
        doThrow(new DataIntegrityViolationException("Simulated instance failure"))
            .when(instances).saveAndFlush(any(RelyingPartyInstance.class));

        assertThrows(DataIntegrityViolationException.class,
            () -> service.createRelyingParty("234567890", "Rollback service", List.of(), List.of()));

        assertEquals(partyCount, parties.count());
        assertEquals(serviceCount, services.count());
        assertEquals(instanceCount, instances.count());
        assertFalse(parties.existsByOrgno("234567890"));
    }

    @Test
    void deletingServiceCascadesInstanceChildrenButPreservesParty() {
        WalletRelyingParty party = EntityGenerator.generateWalletRelyingParty(1);
        RelyingPartyInstance instance = EntityGenerator.instances(party).getFirst();
        var entitlement = new RelyingPartyEntitlement("test-entitlement");
        var issuer = new IssuerCertificate(CertificatesGenerator.generateX509Certificate(), "issuer", entitlement);
        entitlement.addIssuerCertificate(issuer);
        instance.setRelyingPartyEntitlements(List.of(entitlement));
        var access = EntityGenerator.generateCertificate();
        instance.setAccessCertificates(List.of(access));
        parties.saveAndFlush(party);
        var serviceId = instance.getWalletRelyingPartyService().getId();

        service.deleteRelyingParty(instance.getId());

        assertFalse(services.existsById(serviceId));
        assertFalse(instances.existsById(instance.getId()));
        assertFalse(accessCertificates.existsById(access.getId()));
        assertFalse(issuerCertificates.existsById(issuer.getId()));
        WalletRelyingParty remainingParty = parties.findById(party.getId()).orElseThrow();
        assertTrue(remainingParty.getServices().isEmpty());
    }
}
