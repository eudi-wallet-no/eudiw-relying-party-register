package no.idporten.eudiw.rp.admin.web.form;

import no.idporten.eudiw.rp.admin.integrations.registerservice.V2RegisterClient;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class IntendedUseMapper {

    public static final String PRIVACY_STATEMENT_TYPE = "http://data.europa.eu/eudi/policy/privacy-statement";

    private static final String SD_JWT_FORMAT = "dc+sd-jwt";
    private static final JsonMapper CLAIM_PATH_MAPPER = JsonMapper.builder()
        .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
        .build();

    private IntendedUseMapper() {
    }

    public static MappingResult toCreateRequest(IntendedUseForm form) {
        Objects.requireNonNull(form, "The form must be provided.");
        List<IntendedUseForm.Credential> rows = form.getCredentials();
        if (rows == null || rows.isEmpty()) {
            return MappingResult.error("credentials", "required", "Legg til minst eitt bevis.");
        }

        List<V2RegisterClient.IntendedUseCredential> credentials = new ArrayList<>();
        for (int index = 0; index < rows.size(); index++) {
            var row = rows.get(index);
            String field = "credentials[" + index + "].";
            if (row == null || row.getName() == null || row.getName().isBlank()) {
                return MappingResult.error(field + "name", "required", "Skriv eit visingsnamn for beviset.");
            }
            if (row.getName().strip().length() > 255) {
                return MappingResult.error(field + "name", "nameTooLong",
                    "Visingsnamnet for beviset kan ikkje ha meir enn 255 teikn.");
            }
            if (row.getVct() == null || row.getVct().isBlank()) {
                return MappingResult.error(field + "vct", "required", "Skriv ein VCT.");
            }
            if (row.getClaimPaths() == null || row.getClaimPaths().isBlank()) {
                return MappingResult.error(field + "claimPaths", "claimsRequired", "Skriv minst éin attributtsti.");
            }

            List<Map<String, Object>> claims = new ArrayList<>();
            String[] lines = row.getClaimPaths().strip().split("\\r\\n|\\r|\\n", -1);
            for (int line = 0; line < lines.length; line++) {
                List<?> path = parsePath(lines[line]);
                if (path == null) {
                    return MappingResult.error(field + "claimPaths", "invalidClaims",
                        "Linje " + (line + 1) + ": skriv ei JSON-liste med minst eitt element. Bruk tekst som ikkje er tom eller berre mellomrom, null eller heiltal frå 0 og oppover.");
                }
                claims.add(Map.of("path", path));
            }
            credentials.add(new V2RegisterClient.IntendedUseCredential(
                SD_JWT_FORMAT, row.getName().strip(),
                Map.of("vct_values", List.of(row.getVct().strip())), List.copyOf(claims)));
        }

        return createRequest(form, credentials);
    }

    private static List<?> parsePath(String line) {
        try {
            Object value = CLAIM_PATH_MAPPER.readValue(line, Object.class);
            if (value instanceof List<?> path && !path.isEmpty()
                && path.stream().allMatch(IntendedUseMapper::isValidPathSegment)) {
                return path;
            }
        } catch (JacksonException exception) {
            return null;
        }

        return null;
    }

    private static boolean isValidPathSegment(Object segment) {
        return segment == null
            || (segment instanceof String text && !text.isBlank())
            || (segment instanceof Integer integer && integer >= 0)
            || (segment instanceof Long integer && integer >= 0)
            || (segment instanceof BigInteger integer && integer.signum() >= 0);
    }

    private static MappingResult createRequest(
        IntendedUseForm form,
        List<V2RegisterClient.IntendedUseCredential> credentials
    ) {
        List<V2RegisterClient.IntendedUsePurpose> purpose = mapPurposes(form.getPurpose());
        List<V2RegisterClient.IntendedUsePolicy> privacyPolicy = List.of(
            new V2RegisterClient.IntendedUsePolicy(PRIVACY_STATEMENT_TYPE, form.getPrivacyPolicyUri().strip()));

        return MappingResult.success(new V2RegisterClient.CreateIntendedUseRequest(
            purpose,
            privacyPolicy,
            List.copyOf(credentials)));
    }

    private static List<V2RegisterClient.IntendedUsePurpose> mapPurposes(List<IntendedUseForm.Purpose> purposes) {
        if (purposes == null) {
            return List.of();
        }

        return purposes.stream()
            .filter(entry -> entry != null
                && entry.getLang() != null && !entry.getLang().isBlank()
                && entry.getContent() != null && !entry.getContent().isBlank())
            .map(entry -> new V2RegisterClient.IntendedUsePurpose(entry.getLang().strip(), entry.getContent().strip()))
            .toList();
    }

    public record MappingResult(
        V2RegisterClient.CreateIntendedUseRequest request,
        String errorCode,
        String errorMessage,
        String errorField
    ) {
        public static MappingResult success(V2RegisterClient.CreateIntendedUseRequest request) {
            return new MappingResult(request, null, null, null);
        }

        public static MappingResult error(String errorField, String errorCode, String errorMessage) {
            return new MappingResult(null, errorCode, errorMessage, errorField);
        }

        public boolean hasError() {
            return errorCode != null;
        }
    }
}
