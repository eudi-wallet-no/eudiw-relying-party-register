package no.eudiw.rp.register.repository;

import no.eudiw.rp.register.domain.IntendedUse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IntendedUseRepository extends JpaRepository<IntendedUse, UUID> {

    @Query("""
        SELECT intendedUse FROM IntendedUse intendedUse
        WHERE function('BINARY', intendedUse.intendedUseIdentifier) = function('BINARY', :identifier)
        """)
    List<IntendedUse> findAllExactByIntendedUseIdentifier(@Param("identifier") String identifier);
}
