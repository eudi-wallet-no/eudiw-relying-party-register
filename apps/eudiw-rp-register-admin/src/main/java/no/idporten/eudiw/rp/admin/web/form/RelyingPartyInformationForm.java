package no.idporten.eudiw.rp.admin.web.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import no.idporten.eudiw.rp.admin.integrations.registerservice.V2RegisterClient;

import java.net.URI;
import java.util.List;

@Getter
@Setter
public class RelyingPartyInformationForm {

    private String tradeName;

    @NotBlank(message = "Skriv inn land.")
    @Pattern(regexp = "[A-Z]{2}", message = "Skriv inn ein landskode med to store bokstavar.")
    private String country;

    private String postalAddress;
    private String email;
    private String phone;
    private String informationUri;

    public static RelyingPartyInformationForm from(V2RegisterClient.RelyingParty relyingParty) {
        var form = new RelyingPartyInformationForm();
        form.setTradeName(relyingParty.tradeName());
        form.setCountry(relyingParty.country());
        form.setPostalAddress(toInputValue(relyingParty.postalAddress()));
        form.setEmail(toInputValue(relyingParty.email()));
        form.setPhone(toInputValue(relyingParty.phone()));
        form.setInformationUri(toInputValue(relyingParty.informationUri()));
        return form;
    }

    public V2RegisterClient.RelyingPartyInformation toRequest(V2RegisterClient.RelyingParty current) {
        return new V2RegisterClient.RelyingPartyInformation(
            tradeName == null || tradeName.isBlank() ? null : tradeName.strip(),
            country,
            toValues(postalAddress, current.postalAddress()),
            toValues(email, current.email()),
            toValues(phone, current.phone()),
            toValues(informationUri, current.informationUri()));
    }

    public boolean hasValidInformationUris() {
        return informationUri == null || informationUri.isBlank() || isAbsoluteUri(informationUri.strip());
    }

    private static boolean isAbsoluteUri(String value) {
        try {
            return URI.create(value).isAbsolute();
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    // TODO: Legg til støtte for fleire verdiar per liste. Førebels kan berre lister med éin verdi endrast.
    private static String toInputValue(List<String> values) {
        return values == null || values.isEmpty() ? "" : values.getFirst();
    }

    // null: feltet skal ikkje oppdaterast (gjeld også lister med fleire verdiar, som er skrivebeskytta).
    // Tom verdi: feltet skal nullast ut i registeret.
    private static List<String> toValues(String value, List<String> current) {
        if (value == null || (current != null && current.size() > 1)) {
            return null;
        }
        return value.isBlank() ? List.of() : List.of(value.strip());
    }
}
