package no.idporten.eudiw.trustlist.domain.etsi602;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import no.idporten.eudiw.trustlist.domain.TSName;

import java.math.BigInteger;
import java.net.URI;
import java.time.ZonedDateTime;

public record ListAndSchemeInformation602(
        @NotNull @Valid TSName schemeName,
        @Positive @NotNull BigInteger sequenceNumber,
        @NotNull ZonedDateTime listIssueDateTime,
        @NotNull @Valid URI loteType,
        @NotBlank String uri,
        @NotNull @Valid URI statusDeterminationApproach,
        @NotBlank String schemeTypeCommunityRules
        ) {
    public ListAndSchemeInformation602 {
        if (listIssueDateTime == null || listIssueDateTime.isAfter(ZonedDateTime.now())) {
            throw new IllegalArgumentException("List issue date time must not be null and not in the future");
        }
    }

}
