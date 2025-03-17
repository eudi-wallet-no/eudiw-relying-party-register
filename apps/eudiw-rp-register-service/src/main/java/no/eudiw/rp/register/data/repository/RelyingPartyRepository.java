package no.eudiw.rp.register.data.repository;

import no.eudiw.rp.register.data.entity.RelyingParty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RelyingPartyRepository
    extends JpaRepository<RelyingParty, UUID> {

    // NOTE: orgno column is unique in relying_party.
    Optional<RelyingParty> findByOrgno(String orgno);
    boolean existsByOrgno(String orgno);

    List<RelyingParty> findAllByDeleted(boolean deleted);

    // orgno required; publicSector optional; includeActive optional (default false).
    @Query("""
        SELECT r
        FROM RelyingParty r
        WHERE (r.orgno = :orgno AND
               (:publicSector IS NULL OR r.publicSector = :publicSector) AND
               (r.active OR :includeInactive = true))
    """)
    List<RelyingParty> findByOrgnoAndOptionalPublicSector(
        @Param("orgno")           String orgno,
        @Param("publicSector")    Boolean publicSector,
        @Param("includeInactive") Boolean includeInactive);
}


