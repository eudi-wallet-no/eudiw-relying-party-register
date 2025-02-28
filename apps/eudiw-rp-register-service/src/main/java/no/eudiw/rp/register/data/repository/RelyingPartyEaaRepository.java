package no.eudiw.rp.register.data.repository;

import no.eudiw.rp.register.data.entity.RelyingParty;
import no.eudiw.rp.register.data.entity.RelyingPartyEaa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RelyingPartyEaaRepository
    extends JpaRepository<RelyingPartyEaa, UUID> {

    // find all by FK.
    List<RelyingPartyEaa> findAllByRelyingParty(RelyingParty relyingParty);

    List<RelyingPartyEaa> findAllByNamespace(String namespace);
    List<RelyingPartyEaa> findAllByIntent(String intent);
}
