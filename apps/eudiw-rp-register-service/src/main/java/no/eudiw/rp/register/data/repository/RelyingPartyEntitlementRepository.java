package no.eudiw.rp.register.data.repository;

import no.eudiw.rp.register.data.entity.RelyingParty;
import no.eudiw.rp.register.data.entity.RelyingPartyEntitlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RelyingPartyEntitlementRepository
    extends JpaRepository<RelyingPartyEntitlement, UUID> {

    // find all by FK.
    List<RelyingPartyEntitlement> findAllByRelyingParty(RelyingParty relyingParty);

    List<RelyingPartyEntitlement> findAllByEntitlement(String entitlement);
}
