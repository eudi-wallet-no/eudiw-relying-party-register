package no.eudiw.rp.register.data.repository;

import no.eudiw.rp.register.data.entity.RelyingParty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RelyingPartyRepository
    extends JpaRepository<RelyingParty, UUID> {

    // NOTE: orgno column is unique in relying_party.
    Optional<RelyingParty> findByOrgno(String orgno);
    boolean existsByOrgno(String orgno);

    List<RelyingParty> findAllByName(String name);

    List<RelyingParty> findAllByPublicSector(boolean publicSector);

    List<RelyingParty> findAllByDeleted(boolean deleted);
}
