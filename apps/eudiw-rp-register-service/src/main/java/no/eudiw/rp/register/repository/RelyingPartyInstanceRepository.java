package no.eudiw.rp.register.repository;

import no.eudiw.rp.register.domain.relyingparty.RelyingPartyInstance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

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
          AND (:hideSyntheticOrgnos = FALSE OR party.orgno LIKE '8%' OR party.orgno LIKE '9%')
          AND (
            (SELECT COUNT(DISTINCT entitlement.serviceEntitlement.entitlement)
             FROM RelyingPartyEntitlement entitlement
             WHERE entitlement.relyingPartyInstance = instance
               AND entitlement.serviceEntitlement.entitlement IN :requiredEntitlements
            ) = :numRequiredEntitlements
          )
        """)
    Page<RelyingPartyInstance> searchV2(
        @Param("serviceId") UUID serviceId,
        @Param("orgno") String orgno,
        @Param("searchTerm") String searchTerm,
        @Param("includeInactive") boolean includeInactive,
        @Param("hideSyntheticOrgnos") boolean hideSyntheticOrgnos,
        @Param("requiredEntitlements") Set<String> requiredEntitlements,
        @Param("numRequiredEntitlements") int numRequiredEntitlements,
        Pageable pageable
    );

    @Query("""
        SELECT new no.eudiw.rp.register.repository.RegistrationSummaryRow(
            instance.id, service.serviceTradeName, instance.createdMs, instance.lastUpdatedMs,
            role.id, serviceRole.entitlement)
        FROM RelyingPartyInstance instance
        JOIN instance.walletRelyingPartyService service
        JOIN service.walletRelyingParty party
        LEFT JOIN instance.relyingPartyEntitlements role
        LEFT JOIN role.serviceEntitlement serviceRole
        WHERE party.orgno = :orgno
        ORDER BY instance.createdMs ASC, instance.id ASC
        """)
    List<RegistrationSummaryRow> findRegistrationSummariesByOrgno(@Param("orgno") String orgno);

    @Query(value = """
        SELECT DISTINCT configuration.credential_issuer_url
        FROM wallet_relying_party_service_entitlement configuration
        JOIN wallet_relying_party_service service ON service.id = configuration.wallet_relying_party_service_id
        JOIN wallet_relying_party party ON party.id = service.wallet_relying_party_id
        WHERE service.active = TRUE
          AND party.active = TRUE
          AND configuration.credential_issuer_url IS NOT NULL
          AND EXISTS (
              SELECT 1 FROM relying_party_instance instance
              JOIN relying_party_entitlement entitlement ON entitlement.relying_party_instance_id = instance.id
              WHERE instance.wallet_relying_party_service_id = service.id
                AND instance.active = TRUE
                AND entitlement.service_entitlement_id = configuration.id
          )
        """, nativeQuery = true)
    Set<String> findActiveCredentialIssuerUrls();
}
