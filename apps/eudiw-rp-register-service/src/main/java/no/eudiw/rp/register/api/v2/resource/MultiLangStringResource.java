package no.eudiw.rp.register.api.v2.resource;

import jakarta.validation.constraints.NotBlank;
import no.eudiw.rp.register.domain.MultiLangString;

public record MultiLangStringResource(
    @NotBlank String lang,
    @NotBlank String content
) {
    public MultiLangString toDomain() {
        return new MultiLangString(lang, content);
    }
}
