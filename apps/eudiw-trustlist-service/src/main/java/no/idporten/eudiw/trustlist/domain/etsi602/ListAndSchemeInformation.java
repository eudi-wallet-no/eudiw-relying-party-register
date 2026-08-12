package no.idporten.eudiw.trustlist.domain.etsi602;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import no.idporten.eudiw.trustlist.domain.TSName;
import no.idporten.eudiw.trustlist.domain.TSUri;

import java.math.BigInteger;
import java.net.URI;
import java.time.ZonedDateTime;

public record ListAndSchemeInformation(
        @NotNull @Valid TSName schemeName,
        @Positive @NotNull BigInteger sequenceNumber,
        @NotNull ZonedDateTime listIssueDateTime,
        @NotNull @Valid URI loteType,
        @NotNull @Valid TSUri informationUris,
        @NotNull @Valid URI statusDeterminationApproach,
        @NotBlank String schemeTypeCommunityRules
        ) {
    public ListAndSchemeInformation {
        if (listIssueDateTime == null || listIssueDateTime.isAfter(ZonedDateTime.now())) {
            throw new IllegalArgumentException("List issue date time must not be null and not in the future");
        }
    }

}
