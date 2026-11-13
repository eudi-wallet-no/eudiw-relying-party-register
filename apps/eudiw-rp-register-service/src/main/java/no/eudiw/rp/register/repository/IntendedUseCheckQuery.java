package no.eudiw.rp.register.repository;

import no.eudiw.rp.register.domain.IntendedUse;

import java.util.List;

public interface IntendedUseCheckQuery {
    List<IntendedUse> findAllMatchingCheckFilters(
        String identifier,
        String serviceIdentifier,
        String intendedUseIdentifier,
        String credentialFormat,
        String claimPath
    );
}
