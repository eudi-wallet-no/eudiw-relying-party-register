package no.idporten.eudiw.trustlist.domain;

import java.math.BigInteger;
import java.time.ZonedDateTime;

public record TLSchemeInformation(BigInteger sequenceNumber, ZonedDateTime listIssueDateTime) {
    public TLSchemeInformation {
        if (sequenceNumber == null || sequenceNumber.compareTo(BigInteger.ZERO) < 1) {
            throw new IllegalArgumentException("Sequence number must be a non-negative BigInteger greater than zero");
        }
        if (listIssueDateTime == null || listIssueDateTime.isAfter(ZonedDateTime.now())) {
            throw new IllegalArgumentException("List issue date time must not be null and not in the future");
        }
    }

}
