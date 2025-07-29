package no.idporten.eudiw.ca.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, CertificateId> {

    List<Certificate> findAllByIssuerCaAndRevocationReasonGreaterThan(String issuerCa, int revocationReason);

    default List<Certificate> findRevokedCertificates(String issuerCa) {
        return findAllByIssuerCaAndRevocationReasonGreaterThan(issuerCa, -1);
    }

}
