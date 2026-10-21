package no.eudiw.rp.register.repository;

import no.eudiw.rp.register.domain.WalletRelyingPartyService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface WalletRelyingPartyServiceRepository
    extends JpaRepository<WalletRelyingPartyService, UUID> {

    Page<WalletRelyingPartyService> findAllByWalletRelyingParty_Id(UUID partyId, Pageable pageable);
}
