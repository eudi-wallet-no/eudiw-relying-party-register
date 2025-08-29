package no.eudiw.rp.register.data.repository;

import no.eudiw.rp.register.data.entity.RelyingParty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RelyingPartyRepository
    extends JpaRepository<RelyingParty, UUID> {

    Optional<RelyingParty> findByIdAndDeletedFalse(UUID id);

    List<RelyingParty> findByOrgno(String orgno);
    boolean existsByIdAndDeletedFalse(UUID id);

    @Query("""
        SELECT r
        FROM RelyingParty r
        WHERE (r.orgno LIKE :searchTerm% OR r.name ILIKE :searchTerm%)
          AND r.deleted = FALSE
          AND (r.active = TRUE OR :includeInactive = TRUE)
          AND ((SELECT COUNT(DISTINCT e.entitlement)
                FROM RelyingPartyEntitlement e
                WHERE e.relyingParty = r
                  AND e.entitlement IN :entitlements
               ) = :entitlementCount)
    """)
    Page<RelyingParty> searchQueryPaged(
        @Param("searchTerm")       String searchTerm,
        @Param("includeInactive")  boolean includeInactive,
        @Param("entitlements")     List<String> entitlements,
        @Param("entitlementCount") int entitlementCount,
        Pageable pageable
    );

    default Page<RelyingParty> searchQueryPaged(
        String searchTerm,
        boolean includeInactive,
        List<String> requiredEntitlements,
        Pageable pageable) {
        return searchQueryPaged(searchTerm,
                                includeInactive,
                                requiredEntitlements,
                                new HashSet<>(requiredEntitlements).size(),
                                pageable);
    }

    default List<RelyingParty> searchQuery(String searchTerm, boolean includeInactive) {
        return searchQueryPaged(searchTerm, includeInactive, List.of(),
                                PageRequest.ofSize(Integer.MAX_VALUE))
                   .getContent();
    }
}
