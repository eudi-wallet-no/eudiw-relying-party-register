package no.eudiw.rp.register.data.repository;

import no.eudiw.rp.register.data.entity.RelyingParty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RelyingPartyRepository
    extends JpaRepository<RelyingParty, UUID> {

    Optional<RelyingParty> findByIdAndDeletedFalse(UUID id);

    List<RelyingParty> findByOrgno(String orgno);
    boolean existsByIdAndDeletedFalse(UUID id);

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
              (r.deleted IS FALSE) AND
              (r.active OR :includeInactive IS TRUE)
    """)
    List<RelyingParty> advancedSearchQuery(
        @Param("orgno")           String orgno,
        @Param("name")            String name,
        @Param("publicSector")    Boolean publicSector,
        @Param("includeInactive") boolean includeInactive);

    // simple searching with a combined search term on orgno and name.
    @Query("""
        SELECT r
        FROM RelyingParty r
        WHERE (r.orgno LIKE :searchTerm% OR r.name ILIKE :searchTerm%) AND
              (r.deleted IS FALSE) AND
              (r.active OR :includeInactive IS TRUE)
    """)
    List<RelyingParty> searchQuery(
        @Param("searchTerm")      String searchTerm,
        @Param("includeInactive") boolean includeInactive);

    @Query(
        value = """
            SELECT r
            FROM RelyingParty r
            LEFT JOIN r.relyingPartyEntitlements e
            WHERE (:searchTerm IS NULL OR (r.orgno LIKE :searchTerm% OR r.name ILIKE :searchTerm%))
              AND r.deleted = FALSE
              AND (r.active = TRUE OR :includeInactive = TRUE)
              AND (:filterEntitlements = FALSE OR e.entitlement IN :entitlements)
            GROUP BY r
            HAVING (
                :filterEntitlements = FALSE
                OR COUNT(DISTINCT e.entitlement) = :entitlementCount
            )
            """,
        countQuery = """
            SELECT COUNT(r)
            FROM RelyingParty r
            WHERE (:searchTerm IS NULL OR (r.orgno LIKE :searchTerm% OR r.name ILIKE :searchTerm%))
              AND r.deleted = FALSE
              AND (r.active = TRUE OR :includeInactive = TRUE)
              AND (
                    :filterEntitlements = FALSE
                    OR (
                        SELECT COUNT(DISTINCT e2.entitlement)
                        FROM RelyingPartyEntitlement e2
                        WHERE e2.relyingParty = r
                          AND e2.entitlement IN :entitlements
                    ) = :entitlementCount
                  )
            """
    )
    Page<RelyingParty> advancedSearchQueryPage(
        @Param("searchTerm")         String searchTerm,
        @Param("includeInactive")    boolean includeInactive,
        @Param("entitlements")       List<String> entitlements,
        @Param("filterEntitlements") boolean filterEntitlements,
        @Param("entitlementCount")   int entitlementCount,
        Pageable pageable
    );
}


