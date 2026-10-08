package no.eudiw.rp.register.repository;

import no.eudiw.rp.register.domain.Entitlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EntitlementRepository extends JpaRepository<Entitlement, UUID> {

    List<Entitlement> findAllByActive(boolean active);
    boolean existsByEntitlementAndActive(String entitlement, boolean active);
    boolean existsByEntitlement(String entitlement);
    Optional<Entitlement> findByEntitlement(String entitlement);
}


