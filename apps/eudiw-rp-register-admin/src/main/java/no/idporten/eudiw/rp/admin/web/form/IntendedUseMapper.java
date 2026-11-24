package no.idporten.eudiw.rp.admin.web.form;

import no.idporten.eudiw.rp.admin.integrations.issuermetadata.IssuerMetadataService;
import no.idporten.eudiw.rp.admin.integrations.registerservice.V2RegisterClient;

import java.text.Collator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class IntendedUseMapper {

    public static final String PRIVACY_STATEMENT_TYPE = "http://data.europa.eu/eudi/policy/privacy-statement";

    private IntendedUseMapper() {
    }

    public static List<CredentialOption> createCredentialOptions(
        List<IssuerMetadataService.Definition> definitions
    ) {
        if (definitions == null || definitions.isEmpty()) {
            return List.of();
        }

        Collator norwegianCollator = Collator.getInstance(Locale.forLanguageTag("nn"));
        norwegianCollator.setStrength(Collator.PRIMARY);
        List<IssuerMetadataService.Definition> sortedDefinitions = definitions.stream()
            .sorted((left, right) -> norwegianCollator.compare(left.label(), right.label()))
            .toList();

        List<CredentialOption> options = new ArrayList<>();

        for (IssuerMetadataService.Definition definition : sortedDefinitions) {
            List<ClaimOption> claimOptions = new ArrayList<>();
            for (int index = 0; index < definition.claims().size(); index++) {
                claimOptions.add(new ClaimOption(index, definition.claims().get(index).label(), false));
            }

            options.add(new CredentialOption(
                options.size(),
                definition.id(),
                definition.label(),
                definition.error(),
                List.copyOf(claimOptions)));
        }

        return List.copyOf(options);
    }

    public static MappingResult toCreateRequest(
        IntendedUseForm form,
        List<IssuerMetadataService.Definition> definitions
    ) {
        Objects.requireNonNull(form, "Skjemaet må vere oppgitt.");

        List<IntendedUseForm.CredentialSelection> selections = form.getCredentialSelections();
        if (selections == null || selections.isEmpty()) {
            return MappingResult.error("required", "Vel minst eitt bevis.");
        }

        Map<String, IssuerMetadataService.Definition> definitionsById = indexDefinitions(definitions);
        List<V2RegisterClient.IntendedUseCredential> credentials = new ArrayList<>();

        for (IntendedUseForm.CredentialSelection selection : selections) {
            if (selection == null) {
                return MappingResult.error("invalid", "Bevislista er ugyldig. Hent bevisa på nytt før du lagrar.");
            }

            String credentialId = selection.getCredentialId();
            if (credentialId == null || credentialId.isBlank()) {
                continue;
            }

            IssuerMetadataService.Definition definition = definitionsById.get(credentialId);
            if (definition == null) {
                return MappingResult.error("stale", "Bevislista er endra. Hent bevisa på nytt før du lagrar.");
            }

            if (definition.error() != null) {
                return MappingResult.error("unsupported", "Eitt av dei valde bevisa kan ikkje brukast.");
            }

            List<Integer> selectedClaimIndexes = selectedClaimIndexes(selection, definition);
            if (selectedClaimIndexes == null) {
                return MappingResult.error("invalidClaims", "Attributta for eit valt bevis er ugyldige.");
            }

            if (selectedClaimIndexes.isEmpty()) {
                return MappingResult.error("claimsRequired", "Vel minst eitt attributt for kvart valt bevis.");
            }

            List<Map<String, Object>> claims = selectedClaimIndexes.stream()
                .map(index -> Map.<String, Object>of("path", definition.claims().get(index).path()))
                .toList();
            Map<String, Object> metadata = Map.of("vct_values", List.of(definition.vct()));
            credentials.add(new V2RegisterClient.IntendedUseCredential(
                IssuerMetadataService.SD_JWT_FORMAT,
                definition.label(),
                metadata,
                claims));
        }

        if (credentials.isEmpty()) {
            return MappingResult.error("required", "Vel minst eitt bevis.");
        }

        List<V2RegisterClient.IntendedUsePurpose> purpose = mapPurposes(form.getPurpose());
        List<V2RegisterClient.IntendedUsePolicy> privacyPolicy = List.of(
            new V2RegisterClient.IntendedUsePolicy(PRIVACY_STATEMENT_TYPE, form.getPrivacyPolicyUri().strip()));

        return MappingResult.success(new V2RegisterClient.CreateIntendedUseRequest(
            purpose,
            privacyPolicy,
            List.copyOf(credentials)));
    }

    private static Map<String, IssuerMetadataService.Definition> indexDefinitions(
        List<IssuerMetadataService.Definition> definitions
    ) {
        Map<String, IssuerMetadataService.Definition> definitionsById = new HashMap<>();
        if (definitions != null) {
            definitions.forEach(definition -> definitionsById.put(definition.id(), definition));
        }

        return definitionsById;
    }

    private static List<Integer> selectedClaimIndexes(
        IntendedUseForm.CredentialSelection selection,
        IssuerMetadataService.Definition definition
    ) {
        if (selection.getClaims() == null) {
            return null;
        }

        List<Integer> indexes = selection.getClaims().stream()
            .filter(claim -> claim != null && claim.isSelected())
            .map(IntendedUseForm.ClaimSelection::getIndex)
            .toList();

        boolean hasInvalidIndex = indexes.stream()
            .anyMatch(index -> index == null || index < 0 || index >= definition.claims().size());
        if (hasInvalidIndex || indexes.size() != new HashSet<>(indexes).size()) {
            return null;
        }

        return indexes;
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

    public record CredentialOption(
        int index,
        String id,
        String label,
        String error,
        List<ClaimOption> claims
    ) {
    }

    public record ClaimOption(int index, String label, boolean selected) {
    }

    public record MappingResult(
        V2RegisterClient.CreateIntendedUseRequest request,
        String errorCode,
        String errorMessage
    ) {
        public static MappingResult success(V2RegisterClient.CreateIntendedUseRequest request) {
            return new MappingResult(request, null, null);
        }

        public static MappingResult error(String errorCode, String errorMessage) {
            return new MappingResult(null, errorCode, errorMessage);
        }

        public boolean hasError() {
            return errorCode != null;
        }
    }
}
