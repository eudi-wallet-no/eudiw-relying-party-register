package no.eudiw.rp.register.data.service.certificates;

import lombok.RequiredArgsConstructor;
import no.eudiw.rp.register.api.resource.certificates.RelyingPartyCsrResource;
import no.eudiw.rp.register.api.resource.certificates.RelyingPartyCertificateResource;
import no.eudiw.rp.register.api.resource.certificates.RelyingPartyCertificatesResource;
import no.eudiw.rp.register.data.certificates.PKCS10CertificationRequestConverter;
import no.eudiw.rp.register.data.certificates.X509CertificateConverter;
import no.eudiw.rp.register.data.entity.RelyingParty;
import no.eudiw.rp.register.data.entity.RelyingPartyCertificate;
import no.eudiw.rp.register.data.repository.RelyingPartyCertificateRepository;
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
public class RelyingPartyCertificateService {

    private final RelyingPartyRepository relyingPartyRepository;
    private final RelyingPartyCertificateRepository relyingPartyCertificateRepository;
    private final RestClient caRestClient;

    @Transactional(readOnly = true)
    public RelyingPartyCertificatesResource getCertificatesForRelyingParty(
        UUID relyingPartyId) {
        return new RelyingPartyCertificatesResource(
            relyingPartyRepository
                .findByIdAndDeletedFalse(relyingPartyId)
                .orElseThrow(() -> new NotFoundException("Relying party not found"))
                .getRelyingPartyCertificates()
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
            relyingPartyCertificateRepository
                .findByIdAndRelyingPartyId(certificateId, relyingPartyId)
                .orElseThrow(() -> new NotFoundException("Certificate does not exist"))
        );
    }

    @Transactional
    public RelyingPartyCertificateResource requestAccessCertificateForRelyingParty(
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

        RelyingPartyCertificate certificateEntity =
            new RelyingPartyCertificate(certificate);

        relyingParty.addRelyingPartyCertificate(certificateEntity);
        relyingPartyRepository.saveAndFlush(relyingParty);

        return Converter.toResource(certificateEntity);
    }
}
