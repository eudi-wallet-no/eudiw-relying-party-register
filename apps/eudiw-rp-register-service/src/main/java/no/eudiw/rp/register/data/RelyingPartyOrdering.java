package no.eudiw.rp.register.data;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;

@RequiredArgsConstructor
public enum RelyingPartyOrdering {
    NAME_ASC ("name"),
    ORGNO_ASC ("orgno"),
    CREATED_MS_ASC ("createdMs"),
    LAST_UPDATED_MS_ASC ("lastUpdatedMs"),
    UNSORTED ("unsorted");

    @JsonValue
    private final String byColumn;

    public Sort toSort() {
        return this == UNSORTED ? Sort.unsorted() : Sort.by(byColumn);
    }
}
