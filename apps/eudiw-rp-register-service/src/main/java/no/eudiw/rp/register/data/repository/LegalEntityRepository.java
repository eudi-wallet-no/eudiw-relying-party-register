package no.eudiw.rp.register.data.repository;

import no.eudiw.rp.register.data.entity.LegalEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LegalEntityRepository
    extends JpaRepository<LegalEntity, UUID> {

    Optional<LegalEntity> findById(UUID id);
    Optional<LegalEntity> findByOrgno(String orgno);
    boolean existsById(UUID id);
    boolean existsByOrgno(String orgno);

    @Query("""
    SELECT DISTINCT entity
    FROM LegalEntity entity
    LEFT JOIN entity.relyingPartyInstances inst
    WHERE
      (
        :searchTerm IS NULL OR :searchTerm = '' OR
        entity.name ILIKE %:searchTerm% OR
        entity.orgno ILIKE %:searchTerm% OR
        inst.tradeName ILIKE %:searchTerm%
      )
      AND (entity.active = TRUE OR :includeInactive = TRUE)
      AND (
        (SELECT COUNT(DISTINCT e.entitlement)
         FROM RelyingPartyEntitlement e
         WHERE e.relyingPartyInstance = inst
           AND e.entitlement IN :entitlements
        ) = :entitlementCount
      )
    """)
    Page<LegalEntity> searchRelyingParties(
        @Param("searchTerm") String searchTerm,
        @Param("entitlements") List<String> entitlements,
        @Param("entitlementCount") int entitlementCount,
        @Param("includeInactive") boolean includeInactive,
        Pageable pageable
    );
}
