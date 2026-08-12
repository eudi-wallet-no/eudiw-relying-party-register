package no.idporten.eudiw.rp.register.lookup.web.resource;

import java.util.List;

public record PagedResponse<T>(List<T> content, PageMetadata page) {
    public record PageMetadata(long size, long number, long totalElements, long totalPages) {}
}