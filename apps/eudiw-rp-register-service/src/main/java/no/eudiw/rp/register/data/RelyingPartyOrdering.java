package no.eudiw.rp.register.data;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;

@RequiredArgsConstructor
public enum RelyingPartyOrdering {
    BY_NAME ("name"),
    BY_ORGNO ("orgno"),
    BY_CREATED_MS ("created_ms"),
    BY_LAST_UPDATED_MS ("last_updated_ms"),
    UNSORTED ("unsorted");

    @JsonValue
    private final String byColumn;

    public Sort toSort() {
        return this == UNSORTED ? Sort.unsorted() : Sort.by(byColumn);
    }
}
