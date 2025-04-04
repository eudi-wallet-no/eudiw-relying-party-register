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

    // optional orgno with prefix searching.
    // optional name with case-insensitive prefix searching.
    // optional search sector and inactive-inclusion.
    @Query("""
        SELECT r
        FROM RelyingParty r
        WHERE (:orgno        IS NULL OR r.orgno LIKE :orgno%) AND
              (:name         IS NULL OR r.name ILIKE :name%) AND
              (:publicSector IS NULL OR r.publicSector = :publicSector) AND
              (r.deleted = :deleted) AND
              (r.active OR :includeInactive IS TRUE)
    """)
    List<RelyingParty> searchQuery(
        @Param("orgno")           String orgno,
        @Param("name")            String name,
        @Param("publicSector")    Boolean publicSector,
        @Param("deleted")         Boolean deleted,
        @Param("includeInactive") Boolean includeInactive);
}


