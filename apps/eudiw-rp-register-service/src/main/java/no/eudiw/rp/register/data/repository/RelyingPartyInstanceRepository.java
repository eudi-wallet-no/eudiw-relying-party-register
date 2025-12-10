package no.eudiw.rp.register.data.repository;

import no.eudiw.rp.register.data.entity.RelyingPartyInstance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface RelyingPartyInstanceRepository
    extends JpaRepository<RelyingPartyInstance, UUID> {

    @Query("""
    SELECT DISTINCT rpi
    FROM RelyingPartyInstance rpi
    WHERE
      (rpi.legalEntity.orgno LIKE :searchTerm%
          OR rpi.legalEntity.name ILIKE %:searchTerm%
          OR rpi.tradeName ILIKE %:searchTerm%
      )
      AND (:includeInactive = TRUE OR rpi.active = TRUE)
      AND (:hideSyntheticOrgnos = FALSE
              OR rpi.legalEntity.orgno LIKE '8%'
              OR rpi.legalEntity.orgno LIKE '9%')
      AND (
        (SELECT COUNT(DISTINCT e.entitlement)
         FROM RelyingPartyEntitlement e
         WHERE e.relyingPartyInstance = rpi
           AND e.entitlement IN :requiredEntitlements
        ) = :numRequiredEntitlements
      )
    """)
    Page<RelyingPartyInstance> _rawSearchQuery(
        @Param("searchTerm") @NonNull String searchTerm,
        @Param("requiredEntitlements") @NonNull Set<String> requiredEntitlements,
        @Param("numRequiredEntitlements") int numRequiredEntitlements,
        @Param("includeInactive") boolean includeInactive,
        @Param("hideSyntheticOrgnos") boolean hideSyntheticOrgnos,
        Pageable pageable
    );

    default Page<RelyingPartyInstance> searchRelyingPartyInstances(
        String searchTerm,
        Collection<String> requiredEntitlements,
        boolean includeInactive,
        boolean hideSyntheticOrgnos,
        Pageable pageable) {
        searchTerm = Objects.requireNonNullElse(searchTerm, "");
        requiredEntitlements = Objects.requireNonNullElse(requiredEntitlements, List.of());
        Set<String> uniqueRequiredEntitlements = new HashSet<>(requiredEntitlements);
        return _rawSearchQuery(
            searchTerm,
            uniqueRequiredEntitlements,
            uniqueRequiredEntitlements.size(),
            includeInactive,
            hideSyntheticOrgnos,
            pageable);
    }
}
