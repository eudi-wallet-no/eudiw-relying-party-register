package no.eudiw.rp.register.data.repository;

import no.eudiw.rp.register.data.entity.certificates.IssuerCertificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface IssuerCertificateRepository
    extends JpaRepository<IssuerCertificate, UUID> {
}
