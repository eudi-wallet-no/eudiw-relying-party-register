package no.eudiw.rp.register.api.v2.resource;

import jakarta.validation.constraints.NotBlank;

public record MultiLangStringResource(
    @NotBlank String lang,
    @NotBlank String content
) { }
