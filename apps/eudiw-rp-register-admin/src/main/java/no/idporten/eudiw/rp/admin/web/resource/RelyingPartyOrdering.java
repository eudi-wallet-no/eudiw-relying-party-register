package no.idporten.eudiw.rp.admin.web.resource;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum RelyingPartyOrdering {
    NAME_ASC ("name"),
    ORGNO_ASC ("orgno"),
    CREATED_ASC ("createdMs"),
    LAST_UPDATED_ASC ("lastUpdatedMs"),
    UNSORTED ("unsorted");
    @JsonValue
    private final String byColumn;
}
