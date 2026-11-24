package no.idporten.eudiw.rp.admin.web.form;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Getter
@Setter
public class IntendedUseForm {

    private static final String DEFAULT_PURPOSE_LANGUAGE = "nb";
    private static final Pattern LANGUAGE_TAG = Pattern.compile("[A-Za-z]{2,3}(-[A-Za-z0-9]{1,8})*");

    private List<Purpose> purpose = new ArrayList<>();
    private String privacyPolicyUri;
    private IssuerSource issuerSource = IssuerSource.DEFAULT;
    private String issuerMetadataUrl;
    private String metadataLoadedFor;
    private List<CredentialSelection> credentialSelections = new ArrayList<>();

    public IntendedUseForm() {
    }

    public void addDefaultPurposeRow() {
        if (purpose == null) {
            purpose = new ArrayList<>();
        }

        if (purpose.isEmpty()) {
            purpose.add(new Purpose(DEFAULT_PURPOSE_LANGUAGE, null));
        }
    }

    public boolean hasCompletePurposes() {
        return purpose != null && !purpose.isEmpty() && purpose.stream().allMatch(entry ->
            entry != null && hasText(entry.getLang()) && hasText(entry.getContent()));
    }

    public boolean hasValidPurposeLanguages() {
        return purpose != null && purpose.stream()
            .filter(entry -> entry != null && hasText(entry.getLang()) && hasText(entry.getContent()))
            .allMatch(entry -> LANGUAGE_TAG.matcher(entry.getLang()).matches());
    }

    public boolean hasUniquePurposeLanguages() {
        if (purpose == null) {
            return false;
        }

        var languages = purpose.stream()
            .filter(entry -> entry != null && hasText(entry.getLang()) && hasText(entry.getContent()))
            .map(entry -> entry.getLang().toLowerCase(Locale.ROOT))
            .toList();

        return languages.size() == new HashSet<>(languages).size();
    }

    public boolean hasValidPrivacyPolicyUri() {
        if (!hasText(privacyPolicyUri)) {
            return false;
        }

        try {
            return URI.create(privacyPolicyUri.strip()).isAbsolute();
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Purpose {
        private String lang;
        private String content;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CredentialSelection {
        private String credentialId;
        private List<ClaimSelection> claims = new ArrayList<>();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClaimSelection {
        private Integer index;
        private boolean selected;
    }

    public enum IssuerSource {
        DEFAULT,
        CUSTOM
    }
}
