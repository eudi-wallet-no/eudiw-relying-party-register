package no.eudiw.rp.register.data.service.certificates;

import lombok.RequiredArgsConstructor;
import no.eudiw.rp.register.api.resource.RelyingPartyEntitlementsResource;
import no.eudiw.rp.register.api.resource.certificates.IssuerCsrResource;
import no.eudiw.rp.register.api.resource.certificates.RelyingPartyCsrResource;
import no.eudiw.rp.register.api.resource.certificates.RelyingPartyCertificateResource;
import no.eudiw.rp.register.api.resource.certificates.RelyingPartyCertificatesResource;
import no.eudiw.rp.register.data.certificates.PKCS10CertificationRequestConverter;
import no.eudiw.rp.register.data.certificates.X509CertificateConverter;
import no.eudiw.rp.register.data.entity.*;
import no.eudiw.rp.register.data.repository.EntitlementRepository;
import no.eudiw.rp.register.data.repository.IssuerCertificateRepository;
import no.eudiw.rp.register.data.repository.AccessCertificateRepository;
import no.eudiw.rp.register.data.repository.RelyingPartyRepository;
import no.eudiw.rp.register.data.service.Converter;
import no.eudiw.rp.register.data.service.exception.NotFoundException;
import no.eudiw.rp.register.exception.RegisterServiceException;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.security.cert.X509Certificate;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class RelyingPartyCertificateService {

    private final RelyingPartyRepository relyingPartyRepository;
    private final AccessCertificateRepository accessCertificateRepository;
    private final RestClient caRestClient;
    private final EntitlementRepository entitlementRepository;
    private final IssuerCertificateRepository issuerCertificateRepository;

    @Transactional(readOnly = true)
    public RelyingPartyCertificatesResource getCertificatesForRelyingParty(
        UUID relyingPartyId) {
        return new RelyingPartyCertificatesResource(
            relyingPartyRepository
                .findByIdAndDeletedFalse(relyingPartyId)
                .orElseThrow(() -> new NotFoundException("Relying party not found"))
                .getAccessCertificates()
                .stream()
                .map(Converter::toResource)
                .toList()
        );
    }

    @Transactional(readOnly = true)
    public RelyingPartyCertificateResource getCertificate(
        UUID certificateId, UUID relyingPartyId) {
        // NOTE: it is currently possible to get certificates for RPs where
        // deleted=true through the certificates repository.
        // this might be fixed in a better way when revocation is implemented, but
        // for now, use a separate check to assert RP exists.
        if (!relyingPartyRepository.existsByIdAndDeletedFalse(relyingPartyId)) {
            throw new NotFoundException("Certificate holder does not exist or has been deleted");
        }
        return Converter.toResource(
            accessCertificateRepository
                .findByIdAndRelyingPartyId(certificateId, relyingPartyId)
                .orElseThrow(() -> new NotFoundException("Certificate does not exist"))
        );
    }

    @Transactional(readOnly = true)
    public RelyingPartyCertificateResource getIssuerCertificate(
        UUID certificateId) {
        return Converter.toResource(
            issuerCertificateRepository.findById(certificateId)
                .orElseThrow(() -> new NotFoundException("Certificate does not exist"))
        );
    }

    @Transactional(readOnly = true)
    public RelyingPartyEntitlementsResource getAllIssuerCertificatesFromRelyingParty(UUID relyingPartyId) {
        return new RelyingPartyEntitlementsResource(
            relyingPartyRepository
                .findByIdAndDeletedFalse(relyingPartyId)
                .orElseThrow(() -> new NotFoundException("Relying party not found"))
                .getRelyingPartyEntitlements()
                .stream()
                .map(Converter::toResource)
                .toList()
        );
    }

    @Transactional
    public RelyingPartyCertificateResource requestIssuerCertificate(
        UUID relyingPartyId,
        IssuerCsrResource csrResource
    ) {
        RelyingParty relyingParty = getRelyingParty(relyingPartyId);

        RelyingPartyEntitlement relyingPartyEntitlement = relyingParty.getRelyingPartyEntitlement(csrResource.entitlement())
            .orElseThrow(() -> new NotFoundException("RelyingPartyEntitlement does not exist"));

        Entitlement entitlement = entitlementRepository.findByEntitlement(csrResource.entitlement())
            .orElseThrow(() -> new NotFoundException("Entitlement does not exist"));

        if (entitlement.getCaId() == null) {
            throw new RegisterServiceException("Entitlement does not have a CA ID");
        }

        X509Certificate certificate = getCertificateFromCa(
                csrResource.csr(),
                relyingParty.getOrgno(),
                relyingParty.getName(),
                entitlement.getCaId());

        IssuerCertificate certificateEntity =
            new IssuerCertificate(certificate, relyingPartyEntitlement);

        relyingPartyEntitlement.addIssuerCertificate(certificateEntity);

        return Converter.toResource(issuerCertificateRepository.saveAndFlush(certificateEntity));
    }

    @Transactional
    public RelyingPartyCertificateResource requestAccessCertificateForRelyingParty(
        UUID relyingPartyId, RelyingPartyCsrResource csrResource
    ) {
        RelyingParty relyingParty = getRelyingParty(relyingPartyId);

        X509Certificate certificate = getCertificateFromCa(
                csrResource.csr(),
                relyingParty.getOrgno(),
                relyingParty.getName(),
                "access");

        AccessCertificate certificateEntity =
            new AccessCertificate(certificate, relyingParty);

        accessCertificateRepository.saveAndFlush(certificateEntity);

        return Converter.toResource(certificateEntity);
    }

    private RelyingParty getRelyingParty(UUID relyingPartyId) {
        return relyingPartyRepository
            .findByIdAndDeletedFalse(relyingPartyId)
            .orElseThrow(() -> new NotFoundException("Certificate registree does not exist"));
    }

    private X509Certificate getCertificateFromCa(PKCS10CertificationRequest csr, String orgNo, String name, String caId) {
        String csrPemStr = PKCS10CertificationRequestConverter.convert(csr);
        String certificatePemStr =
            caRestClient.post()
                .uri("/" + caId)
                .body(RelyingPartyCertificateRequest
                    .builder()
                    .orgno(orgNo)
                    .name(name)
                    .csr(csrPemStr)
                    .build())
                .retrieve()
                .toEntity(String.class)
                .getBody();

        if (certificatePemStr == null) {
            throw new RegisterServiceException("Null body in ca-service success response");
        }

        return X509CertificateConverter.convert(certificatePemStr);
    }
}
