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
        JOIN intendedUse.walletRelyingPartyService service
        JOIN service.walletRelyingParty party
        WHERE (
            :identifier IS NULL
            OR function('BINARY', party.orgno) = function('BINARY', :identifier)
            OR function(
                'JSON_CONTAINS',
                party.identifier,
                function('JSON_OBJECT', 'identifier', :identifier),
                '$'
            ) = 1
        )
          AND (
            :serviceIdentifier IS NULL
            OR function('BINARY', service.serviceIdentifier) = function('BINARY', :serviceIdentifier)
        )
          AND (
            :intendedUseIdentifier IS NULL
            OR function('BINARY', intendedUse.intendedUseIdentifier)
                = function('BINARY', :intendedUseIdentifier)
        )
        """)
    List<IntendedUse> findAllMatchingCheckFilters(
        @Param("identifier") String identifier,
        @Param("serviceIdentifier") String serviceIdentifier,
        @Param("intendedUseIdentifier") String intendedUseIdentifier
    );
}
