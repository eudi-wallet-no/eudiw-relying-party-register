package no.idporten.eudiw.ca.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.time.Clock;
import java.util.List;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, CertificateId> {

    Certificate findByIssuerCaAndSerialNo(String issuerCa, String serialNo);
    default Certificate findByIssuerCaAndSerialNo(String issuerCa, BigInteger serialNo) {
        return findByIssuerCaAndSerialNo(issuerCa, SerialNumberUtils.convertToString(serialNo));
    }

    List<Certificate> findAllByIssuerCaAndRevocationReasonGreaterThanAndValidUntilMsGreaterThan(String issuerCa, int revocationReason, long validUntilMs);

    default List<Certificate> findRevokedCertificates(String issuerCa) {
        return findAllByIssuerCaAndRevocationReasonGreaterThanAndValidUntilMsGreaterThan(issuerCa, -1, Clock.systemUTC().millis());
    }

}
