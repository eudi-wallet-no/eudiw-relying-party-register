package no.idporten.eudiw.credential.registry.response.model;

import java.util.List;

public record CredentialValidationError(String name, List<String> errors) { }
