package no.eudiw.rp.register.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@Entity
@Table(name = "wallet_relying_party_service_intended_use_credential")
public class Credential extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "intended_use_id", columnDefinition = "UUID", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private IntendedUse intendedUse;

    @Column(name = "format", nullable = false, columnDefinition = "TEXT")
    private String format;

    @Getter(AccessLevel.NONE)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "meta", nullable = false, columnDefinition = "JSON")
    private Map<String, Object> meta;

    @Getter(AccessLevel.NONE)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "claims", nullable = false, columnDefinition = "JSON")
    private List<Map<String, Object>> claims;

    public Credential(String format, Map<String, Object> meta, List<Map<String, Object>> claims) {
        setCredentialData(format, meta, claims);
    }

    public Map<String, Object> getMeta() {
        return copyMap(meta);
    }

    public List<Map<String, Object>> getClaims() {
        return copyClaims(claims);
    }

    public void setCredentialData(String format, Map<String, Object> meta, List<Map<String, Object>> claims) {
        validateCredentialData(format, meta, claims);
        this.format = format;
        this.meta = copyMap(meta);
        this.claims = copyClaims(claims);
    }

    void setIntendedUse(IntendedUse intendedUse) {
        this.intendedUse = intendedUse;
    }

    private static void validateCredentialData(
        String format,
        Map<String, Object> meta,
        List<Map<String, Object>> claims
    ) {
        if (format == null) {
            throw new IllegalArgumentException("Credential format must be provided");
        }
        if (format.isBlank()) {
            throw new IllegalArgumentException("Credential format must not be blank");
        }
        validateMeta(meta);
        validateClaims(claims);
    }

    private static void validateMeta(Map<String, Object> meta) {
        if (meta == null) {
            throw new IllegalArgumentException("Credential meta must be an object");
        }

        boolean hasVctValues = meta.containsKey("vct_values");
        boolean hasDoctype = meta.containsKey("doctype");
        if (hasVctValues == hasDoctype || meta.size() != 1) {
            throw new IllegalArgumentException("Credential meta must contain exactly one of vct_values or doctype");
        }
        if (hasVctValues && !allTextual(meta.get("vct_values"), true)) {
            throw new IllegalArgumentException("Credential meta vct_values must be an array of strings");
        }
        if (hasDoctype && (!(meta.get("doctype") instanceof String doctype) || doctype.isBlank())) {
            throw new IllegalArgumentException("Credential meta doctype must be a string");
        }
    }

    private static boolean allTextual(Object values, boolean requireNonEmpty) {
        if (!(values instanceof List<?> valueList)) {
            return false;
        }
        if (requireNonEmpty && valueList.isEmpty()) {
            return false;
        }
        for (Object value : valueList) {
            if (!(value instanceof String text) || text.isBlank()) {
                return false;
            }
        }
        return true;
    }

    private static void validateClaims(List<Map<String, Object>> claims) {
        if (claims == null || claims.isEmpty()) {
            throw new IllegalArgumentException("Credential claims must be a non-empty array");
        }

        for (Map<String, Object> claim : claims) {
            if (claim == null) {
                throw new IllegalArgumentException("Each credential claim must be an object");
            }
            Object path = claim.get("path");
            if (!(path instanceof List<?> pathParts) || pathParts.isEmpty()) {
                throw new IllegalArgumentException("Each credential claim must have a non-empty path array");
            }
            for (Object pathPart : pathParts) {
                if (!(pathPart == null || (pathPart instanceof String text && !text.isBlank())
                    || (pathPart instanceof Number number && isNonNegativeInteger(number)))) {
                    throw new IllegalArgumentException(
                        "Credential claim path values must be strings, nulls, or non-negative integers");
                }
            }
        }
    }

    private static boolean isNonNegativeInteger(Number number) {
        if (number instanceof Byte || number instanceof Short || number instanceof Integer || number instanceof Long) {
            return number.longValue() >= 0;
        }
        return number instanceof BigInteger bigInteger && bigInteger.signum() >= 0;
    }

    private static Map<String, Object> copyMap(Map<String, Object> map) {
        Map<String, Object> copy = new LinkedHashMap<>();
        map.forEach((key, value) -> copy.put(key, copyJsonValue(value)));
        return copy;
    }

    private static List<Map<String, Object>> copyClaims(List<Map<String, Object>> claims) {
        List<Map<String, Object>> copy = new ArrayList<>(claims.size());
        claims.forEach(claim -> copy.add(copyMap(claim)));
        return copy;
    }

    private static Object copyJsonValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            map.forEach((key, nestedValue) -> {
                if (!(key instanceof String stringKey)) {
                    throw new IllegalArgumentException("JSON object property names must be strings");
                }
                copy.put(stringKey, copyJsonValue(nestedValue));
            });
            return copy;
        }
        if (value instanceof List<?> list) {
            return list.stream().map(Credential::copyJsonValue).toList();
        }
        if (value == null || value instanceof String || value instanceof Boolean || value instanceof Number) {
            return value;
        }
        throw new IllegalArgumentException("Credential data must contain JSON-compatible values");
    }

    @PrePersist
    @PreUpdate
    protected void validateBeforeWrite() {
        validateCredentialData(format, meta, claims);
    }

    @PostLoad
    protected void validateAfterLoad() {
        validateCredentialData(format, meta, claims);
    }

    protected Credential() { }
}
