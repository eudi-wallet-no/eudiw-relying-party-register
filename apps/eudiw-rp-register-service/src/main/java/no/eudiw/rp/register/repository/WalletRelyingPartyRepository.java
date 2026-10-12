package no.eudiw.rp.register.repository;

import no.eudiw.rp.register.domain.WalletRelyingParty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletRelyingPartyRepository
    extends JpaRepository<WalletRelyingParty, UUID> {

    Optional<WalletRelyingParty> findByOrgno(String orgno);
    boolean existsByOrgno(String orgno);
}
