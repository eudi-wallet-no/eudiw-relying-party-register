package no.eudiw.rp.register.repository;

import jakarta.persistence.LockModeType;
import no.eudiw.rp.register.domain.WalletRelyingParty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletRelyingPartyRepository
    extends JpaRepository<WalletRelyingParty, UUID>, WalletRelyingPartyCollectionQuery {

    Optional<WalletRelyingParty> findByOrgno(String orgno);

    Page<WalletRelyingParty> findAllByOrgno(String orgno, Pageable pageable);

    @Query(value = """
        SELECT party.* FROM wallet_relying_party party
        WHERE party.active = TRUE
          AND (party.orgno = :identifier
            OR JSON_CONTAINS(party.identifier, JSON_OBJECT('identifier', :identifier), '$'))
        """, nativeQuery = true)
    List<WalletRelyingParty> findAllByRegisteredIdentifier(String identifier);

    @Query("""
        SELECT DISTINCT service.id AS serviceId, activeEntitlement.entitlement AS entitlementValue
        FROM WalletRelyingPartyService service
        JOIN service.relyingPartyInstances instance
        JOIN instance.relyingPartyEntitlements activeEntitlement
        WHERE service.walletRelyingParty.id IN :partyIds
          AND instance.active = TRUE
        """)
    List<ServiceEntitlementProjection> findActiveInstanceEntitlementsByPartyIds(
        @Param("partyIds") List<UUID> partyIds
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT party FROM WalletRelyingParty party WHERE party.orgno = :orgno")
    Optional<WalletRelyingParty> findByOrgnoForUpdate(String orgno);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT party FROM WalletRelyingParty party WHERE party.id = :id")
    Optional<WalletRelyingParty> findByIdForUpdate(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT party FROM WalletRelyingParty party WHERE party.id = :id AND party.active = TRUE")
    Optional<WalletRelyingParty> findActiveByIdForUpdate(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT party FROM WalletRelyingParty party
        WHERE party.id = (
            SELECT service.walletRelyingParty.id FROM WalletRelyingPartyService service WHERE service.id = :serviceId
        )
        """)
    Optional<WalletRelyingParty> findByServiceIdForUpdate(UUID serviceId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT party FROM WalletRelyingParty party
        WHERE party.active = TRUE
          AND party.id = (
            SELECT service.walletRelyingParty.id
            FROM WalletRelyingPartyService service
            WHERE service.id = :serviceId AND service.active = TRUE
          )
        """)
    Optional<WalletRelyingParty> findByActiveServiceIdForUpdate(UUID serviceId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT party FROM WalletRelyingParty party
        WHERE party.id = (
            SELECT instance.walletRelyingPartyService.walletRelyingParty.id
            FROM RelyingPartyInstance instance WHERE instance.id = :instanceId
        )
        """)
    Optional<WalletRelyingParty> findByInstanceIdForUpdate(UUID instanceId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT party FROM WalletRelyingParty party
        WHERE party.active = TRUE
          AND party.id = (
            SELECT instance.walletRelyingPartyService.walletRelyingParty.id
            FROM RelyingPartyInstance instance
            JOIN instance.walletRelyingPartyService service
            WHERE instance.id = :instanceId
              AND instance.active = TRUE
              AND service.active = TRUE
          )
        """)
    Optional<WalletRelyingParty> findByActiveInstanceIdForUpdate(UUID instanceId);

    boolean existsByOrgno(String orgno);

    interface ServiceEntitlementProjection {
        UUID getServiceId();
        String getEntitlementValue();
    }
}
