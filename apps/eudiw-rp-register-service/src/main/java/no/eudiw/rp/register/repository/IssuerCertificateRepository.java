package no.eudiw.rp.register.repository;

import no.eudiw.rp.register.domain.certificates.IssuerCertificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IssuerCertificateRepository
    extends JpaRepository<IssuerCertificate, UUID> {

    Optional<IssuerCertificate> findByIdAndEntitlementRelyingPartyInstanceId(
        UUID id, UUID relyingPartyInstanceId);
}
