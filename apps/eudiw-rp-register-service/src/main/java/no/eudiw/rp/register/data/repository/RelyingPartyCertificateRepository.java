package no.eudiw.rp.register.data.repository;

import no.eudiw.rp.register.data.entity.RelyingPartyCertificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RelyingPartyCertificateRepository
    extends JpaRepository<RelyingPartyCertificate, UUID> {

    @Query("""
        SELECT cert
        FROM RelyingPartyCertificate cert
        WHERE cert.id = :certId AND cert.relyingParty.id = :relyingPartyId
    """)
    Optional<RelyingPartyCertificate> findByIdAndRelyingPartyId(
        @Param("certId")         UUID certId,
        @Param("relyingPartyId") UUID relyingPartyId);
}
