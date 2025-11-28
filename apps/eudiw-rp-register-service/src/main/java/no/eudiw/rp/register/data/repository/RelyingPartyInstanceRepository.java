package no.eudiw.rp.register.data.repository;

import no.eudiw.rp.register.data.entity.RelyingPartyInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RelyingPartyInstanceRepository
    extends JpaRepository<RelyingPartyInstance, UUID> {

    Optional<RelyingPartyInstance> findById(UUID id);

    boolean existsById(UUID id);
}
