package no.eudiw.rp.register.data.service.accesscertificates;

import lombok.RequiredArgsConstructor;
import no.eudiw.rp.register.api.resource.accesscertificates.RelyingPartyCsrResource;
import no.eudiw.rp.register.api.resource.accesscertificates.RelyingPartyAccessCertificateResource;
import no.eudiw.rp.register.api.resource.accesscertificates.RelyingPartyAccessCertificatesResource;
import no.eudiw.rp.register.data.accesscertificates.PKCS10CertificationRequestConverter;
import no.eudiw.rp.register.data.accesscertificates.X509CertificateConverter;
import no.eudiw.rp.register.data.entity.RelyingParty;
import no.eudiw.rp.register.data.entity.RelyingPartyAccessCertificate;
import no.eudiw.rp.register.data.repository.RelyingPartyAccessCertificateRepository;
import no.eudiw.rp.register.data.repository.RelyingPartyRepository;
import no.eudiw.rp.register.data.service.Converter;
import no.eudiw.rp.register.data.service.exception.NotFoundException;
import no.eudiw.rp.register.exception.RegisterServiceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.security.cert.X509Certificate;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class RelyingPartyCertificatesService {

    private final RelyingPartyRepository relyingPartyRepository;
    private final RelyingPartyAccessCertificateRepository relyingPartyAccessCertificateRepository;
    private final RestClient caRestClient;

    @Transactional(readOnly = true)
    public RelyingPartyAccessCertificatesResource getAccessCertificatesForRelyingParty(
        UUID relyingPartyId) {
        return new RelyingPartyAccessCertificatesResource(
            relyingPartyRepository
                .findById(relyingPartyId)
                .orElseThrow(() -> new NotFoundException("Relying party not found"))
                .getRelyingPartyAccessCertificates()
                .stream()
                .map(Converter::toResource)
                .toList()
        );
    }

    @Transactional(readOnly = true)
    public RelyingPartyAccessCertificateResource getAccessCertificate(
        UUID certificateId, UUID relyingPartyId) {
        return Converter.toResource(
            relyingPartyAccessCertificateRepository
                .findByIdAndRelyingPartyId(certificateId, relyingPartyId)
                .orElseThrow(() -> new NotFoundException("Access certificate does not exist"))
        );
    }

    @Transactional
    public RelyingPartyAccessCertificateResource requestCertificateForRelyingParty(
        UUID relyingPartyId, RelyingPartyCsrResource csrResource) {

        RelyingParty relyingParty =
            relyingPartyRepository
                .findByIdAndDeletedFalse(relyingPartyId)
                .orElseThrow(() -> new NotFoundException("Certificate registree does not exist"));

        String csrPemStr = PKCS10CertificationRequestConverter.convert(csrResource.csr());
        String certificatePemStr =
            caRestClient.post()
                        .uri("/access/" + relyingParty.getOrgno())
                        .body(csrPemStr)
                        .retrieve()
                        .toEntity(String.class)
                        .getBody();

        if (certificatePemStr == null) {
            // NOTE: should never happen since ca-service API specifies a body
            // on success, and on failure the ResponseErrorHandler triggers.
            throw new RegisterServiceException("Null body in ca-service success response");
        }

        X509Certificate certificate =
            X509CertificateConverter.convert(certificatePemStr);

        RelyingPartyAccessCertificate certificateEntity =
            new RelyingPartyAccessCertificate(certificate);

        relyingParty.addRelyingPartyAccessCertificate(certificateEntity);
        relyingPartyRepository.saveAndFlush(relyingParty);

        return Converter.toResource(certificateEntity);
    }
}
