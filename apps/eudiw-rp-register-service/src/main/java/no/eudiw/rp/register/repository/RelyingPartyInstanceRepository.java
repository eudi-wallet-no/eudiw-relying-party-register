package no.eudiw.rp.register.repository;

import no.eudiw.rp.register.domain.relyingparty.RelyingPartyInstance;
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
        SELECT instance FROM RelyingPartyInstance instance
        JOIN instance.walletRelyingPartyService service
        JOIN service.walletRelyingParty party
        WHERE instance.id = :id
          AND instance.active = TRUE
          AND service.active = TRUE
          AND party.active = TRUE
        """)
    Optional<RelyingPartyInstance> findActiveRegistrationById(@Param("id") UUID id);

    @Query("""
        SELECT DISTINCT instance
        FROM RelyingPartyInstance instance
        JOIN instance.walletRelyingPartyService service
        JOIN service.walletRelyingParty party
        LEFT JOIN party.legalPerson legalPerson
        WHERE (:serviceId IS NULL OR service.id = :serviceId)
          AND (:orgno IS NULL OR party.orgno = :orgno)
          AND (:searchTerm = '' OR party.orgno LIKE CONCAT('%', :searchTerm, '%')
              OR party.tradeName ILIKE CONCAT('%', :searchTerm, '%')
              OR service.serviceTradeName ILIKE CONCAT('%', :searchTerm, '%')
              OR cast(function('json_value', legalPerson.legalName, '$[0]') AS String)
                    ILIKE CONCAT('%', :searchTerm, '%'))
          AND (:includeInactive = TRUE OR
              (instance.active = TRUE AND service.active = TRUE AND party.active = TRUE))
          AND (
            (SELECT COUNT(DISTINCT entitlement.entitlement)
             FROM RelyingPartyEntitlement entitlement
             WHERE entitlement.relyingPartyInstance = instance
               AND entitlement.entitlement IN :requiredEntitlements
            ) = :numRequiredEntitlements
          )
        """)
    Page<RelyingPartyInstance> searchV2(
        @Param("serviceId") UUID serviceId,
        @Param("orgno") String orgno,
        @Param("searchTerm") String searchTerm,
        @Param("includeInactive") boolean includeInactive,
        @Param("requiredEntitlements") Set<String> requiredEntitlements,
        @Param("numRequiredEntitlements") int numRequiredEntitlements,
        Pageable pageable
    );

    @Query("""
        SELECT entitlement.credentialIssuerUrl
        FROM RelyingPartyEntitlement entitlement
        JOIN entitlement.relyingPartyInstance instance
        JOIN instance.walletRelyingPartyService service
        JOIN service.walletRelyingParty party
        WHERE instance.active = TRUE
          AND service.active = TRUE
          AND party.active = TRUE
          AND entitlement.credentialIssuerUrl IS NOT NULL
        """)
    Set<String> findActiveCredentialIssuerUrls();

    @Query("""
    SELECT DISTINCT rpi
    FROM RelyingPartyInstance rpi
    LEFT JOIN rpi.walletRelyingPartyService.walletRelyingParty.legalPerson legalPerson
    WHERE
      (rpi.walletRelyingPartyService.walletRelyingParty.orgno LIKE :searchTerm%
          OR rpi.walletRelyingPartyService.walletRelyingParty.tradeName ILIKE %:searchTerm%
          OR (rpi.walletRelyingPartyService.walletRelyingParty.tradeName IS NULL
              AND cast(function('json_value', legalPerson.legalName, '$[0]') AS String) ILIKE %:searchTerm%)
          OR rpi.walletRelyingPartyService.serviceTradeName ILIKE %:searchTerm%
      )
      AND (:includeInactive = TRUE OR rpi.active = TRUE)
      AND (:hideSyntheticOrgnos = FALSE
              OR rpi.walletRelyingPartyService.walletRelyingParty.orgno LIKE '8%'
              OR rpi.walletRelyingPartyService.walletRelyingParty.orgno LIKE '9%')
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
