package no.idporten.eudiw.trustlist.domain.etsi612;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import no.idporten.eudiw.trustlist.domain.TSName;
import no.idporten.eudiw.trustlist.domain.TSUri;

import java.math.BigInteger;
import java.time.ZonedDateTime;

public record TLSchemeInformation(@NotNull @Valid TSName schemeName, @Positive @NotNull BigInteger sequenceNumber, @NotNull ZonedDateTime listIssueDateTime, @NotNull @Valid TSUri informationUris) {
    public TLSchemeInformation {
        if (listIssueDateTime == null || listIssueDateTime.isAfter(ZonedDateTime.now())) {
            throw new IllegalArgumentException("List issue date time must not be null and not in the future");
        }
    }

}
