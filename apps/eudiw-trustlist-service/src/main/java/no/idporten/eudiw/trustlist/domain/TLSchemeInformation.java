package no.idporten.eudiw.trustlist.domain;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigInteger;
import java.time.ZonedDateTime;

public record TLSchemeInformation(@NotEmpty String schemeName, @Positive @NotNull BigInteger sequenceNumber, @NotNull ZonedDateTime listIssueDateTime) {
    public TLSchemeInformation {
        if (listIssueDateTime == null || listIssueDateTime.isAfter(ZonedDateTime.now())) {
            throw new IllegalArgumentException("List issue date time must not be null and not in the future");
        }
    }

}
