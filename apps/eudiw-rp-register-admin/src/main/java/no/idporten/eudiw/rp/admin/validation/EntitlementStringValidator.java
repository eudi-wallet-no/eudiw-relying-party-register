package no.idporten.eudiw.rp.admin.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import no.idporten.eudiw.rp.admin.data.RelyingPartyEntitlement;

public class EntitlementStringValidator
    implements ConstraintValidator<EntitlementConstraint, String> {
    @Override
    public boolean isValid(String entitlementStr, ConstraintValidatorContext ctx) {
        return RelyingPartyEntitlement.isValidEntitlementString(entitlementStr);
    }
    @Override
    public void initialize(EntitlementConstraint entitlementConstraint) {}
}
