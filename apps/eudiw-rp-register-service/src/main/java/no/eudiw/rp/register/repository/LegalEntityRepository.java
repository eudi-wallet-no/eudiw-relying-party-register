package no.eudiw.rp.register.repository;

import no.eudiw.rp.register.domain.LegalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LegalEntityRepository
    extends JpaRepository<LegalEntity, UUID> {

    Optional<LegalEntity> findByOrgno(String orgno);
    boolean existsByOrgno(String orgno);
}
