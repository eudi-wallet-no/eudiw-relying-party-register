package no.eudiw.rp.register.data.repository;

import no.eudiw.rp.register.data.entity.AccessCertificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccessCertificateRepository
    extends JpaRepository<AccessCertificate, UUID> {

    @Query("""
        SELECT cert
        FROM AccessCertificate cert
        WHERE cert.id = :certId AND cert.relyingPartyInstance.id = :relyingPartyId
    """)
    Optional<AccessCertificate> findByIdAndRelyingPartyId(
        @Param("certId")         UUID certId,
        @Param("relyingPartyId") UUID relyingPartyId);
}
