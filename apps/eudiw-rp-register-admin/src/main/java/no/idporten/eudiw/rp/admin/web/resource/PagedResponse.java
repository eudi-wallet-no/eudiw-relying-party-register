package no.idporten.eudiw.rp.admin.web.resource;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record PagedResponse<T>(
    List<T> content,
    PageMetadata page) {

    public record PageMetadata(
        @JsonProperty(required = true) long size,
        @JsonProperty(required = true) long number,
        @JsonProperty(required = true) long totalElements,
        @JsonProperty(required = true) long totalPages
    ) { }
}
