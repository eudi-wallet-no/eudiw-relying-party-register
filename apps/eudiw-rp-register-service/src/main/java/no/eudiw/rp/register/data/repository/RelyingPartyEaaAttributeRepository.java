package no.eudiw.rp.register.data.repository;

import no.eudiw.rp.register.data.entity.RelyingPartyEaa;
import no.eudiw.rp.register.data.entity.RelyingPartyEaaAttribute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RelyingPartyEaaAttributeRepository
    extends JpaRepository<RelyingPartyEaaAttribute, UUID> {

    // find all by FK.
    List<RelyingPartyEaaAttribute> findAllByRelyingPartyEaa(RelyingPartyEaa relyingPartyEaa);
}
