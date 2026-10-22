package no.eudiw.rp.register.api.v2.resource;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import no.eudiw.rp.register.domain.Credential;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

public record CredentialResource(
    @NotBlank String format,
    @NotNull Map<String, Object> meta,
    @NotEmpty List<@NotNull Map<String, Object>> claims
) {
    @AssertTrue(message = "invalid_credential_meta")
    public boolean hasValidMeta() {
        if (meta == null || meta.size() != 1) {
            return false;
        }
        Object vctValues = meta.get("vct_values");
        if (vctValues instanceof List<?> values) {
            return !values.isEmpty() && values.stream()
                .allMatch(value -> value instanceof String text && !text.isBlank());
        }
        return meta.get("doctype") instanceof String doctype && !doctype.isBlank();
    }

    @AssertTrue(message = "invalid_credential_claims")
    public boolean hasValidClaims() {
        return claims != null && !claims.isEmpty() && claims.stream().allMatch(claim -> {
            if (claim == null) {
                return false;
            }
            Object path = claim.get("path");
            if (!(path instanceof List<?> pathParts) || pathParts.isEmpty()) {
                return false;
            }
            return pathParts.stream().allMatch(part ->
                part == null
                    || (part instanceof String text && !text.isBlank())
                    || (part instanceof Integer integerValue && integerValue >= 0)
                    || (part instanceof Long longValue && longValue >= 0)
                    || (part instanceof BigInteger bigIntegerValue && bigIntegerValue.signum() >= 0)
            );
        });
    }

    public Credential toDomain() {
        return new Credential(format, meta, claims);
    }
}
