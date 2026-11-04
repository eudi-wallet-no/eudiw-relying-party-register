package no.idporten.eudiw.rp.admin.web.form;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import no.idporten.eudiw.rp.admin.integrations.registerservice.V2RegisterClient;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyResource;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Getter
@Setter
public class RelyingPartyServiceInformationForm {

    private static final String DEFAULT_DESCRIPTION_LANGUAGE = "nb";
    private static final Pattern LANGUAGE_TAG = Pattern.compile("[A-Za-z]{2,3}(-[A-Za-z0-9]{1,8})*");

    @NotBlank(message = "Skriv inn tenestenamn.")
    private String serviceTradeName;
    private String serviceIdentifier;
    private List<Description> descriptions = new ArrayList<>();
    private String supportUri;
    private String email;
    private String phone;

    public static RelyingPartyServiceInformationForm from(RelyingPartyResource registration) {
        var service = registration.relyingPartyService();
        var form = new RelyingPartyServiceInformationForm();
        form.setServiceTradeName(registration.tradeName());

        if (service != null) {
            form.setServiceIdentifier(service.serviceIdentifier());
            form.setSupportUri(service.supportUri());
            form.setEmail(service.email());
            form.setPhone(service.phone());

            if (service.srvDescription() != null) {
                service.srvDescription().forEach(item ->
                    form.getDescriptions().add(new Description(item.lang(), item.content())));
            }
        }

        form.addDefaultDescriptionRow();
        return form;
    }

    public V2RegisterClient.ServiceInformation toRequest() {
        var srvDescription = descriptionsWithText().stream()
            .map(item -> new RelyingPartyResource.MultiLangStringDetails(item.getLang(), item.getContent()))
            .toList();

        return new V2RegisterClient.ServiceInformation(
            serviceTradeName, serviceIdentifier, srvDescription, supportUri, email, phone);
    }

    public void addDefaultDescriptionRow() {
        if (descriptions.isEmpty()) {
            descriptions.add(new Description(DEFAULT_DESCRIPTION_LANGUAGE, null));
        }
    }

    public boolean hasDescription() {
        return !descriptionsWithText().isEmpty();
    }

    public boolean hasCompleteDescriptions() {
        return descriptions.stream().noneMatch(item -> hasText(item.getLang()) != hasText(item.getContent()));
    }

    public boolean hasValidDescriptionLanguages() {
        return descriptionsWithText().stream().allMatch(item -> LANGUAGE_TAG.matcher(item.getLang()).matches());
    }

    public boolean hasUniqueDescriptionLanguages() {
        var languages = descriptionsWithText().stream()
            .map(item -> item.getLang().toLowerCase(Locale.ROOT))
            .toList();

        return languages.size() == new HashSet<>(languages).size();
    }

    public boolean hasContactInformation() {
        return hasText(supportUri) || hasText(email) || hasText(phone);
    }

    public boolean hasValidSupportUri() {
        return !hasText(supportUri) || isAbsoluteUri(supportUri.strip());
    }

    private List<Description> descriptionsWithText() {
        return descriptions.stream()
            .filter(item -> hasText(item.getLang()) && hasText(item.getContent()))
            .toList();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static boolean isAbsoluteUri(String value) {
        try {
            return URI.create(value).isAbsolute();
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Description {
        private String lang;
        private String content;
    }
}
