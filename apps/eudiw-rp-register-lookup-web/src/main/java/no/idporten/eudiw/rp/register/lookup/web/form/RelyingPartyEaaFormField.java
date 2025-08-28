package no.idporten.eudiw.rp.register.lookup.web.form;

import lombok.*;
import no.idporten.eudiw.rp.register.lookup.validation.SaneStringConstraint;
import no.idporten.eudiw.rp.register.lookup.web.resource.RelyingPartyEaaResource;


@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class RelyingPartyEaaFormField {
    @SaneStringConstraint
    private String namespace;
    @SaneStringConstraint
    private String intent;

    public boolean isSet() {
        return namespace != null && intent != null;
    }

    public RelyingPartyEaaResource toResource() {
        return new RelyingPartyEaaResource(this.namespace, this.intent);
    }

    public static RelyingPartyEaaFormField fromResource(RelyingPartyEaaResource resource) {
        return new RelyingPartyEaaFormField(resource.namespace(), resource.intent());
    }
}
