package no.eudiw.rp.register.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ValidatorFactory;
import no.eudiw.rp.register.api.resource.CreateRelyingPartyResource;
import static no.eudiw.rp.register.testdata.ResourceGenerator.*;

import no.eudiw.rp.register.api.resource.RelyingPartyEaaResource;
import no.eudiw.rp.register.api.resource.RelyingPartyEntitlementResource;
import no.eudiw.rp.register.api.resource.RelyingPartyResource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("When applying jakarta constraint validation to API resources")
public class ValidationTests {

    @Autowired
    private ValidatorFactory validatorFactory;


    private <T> Set<ConstraintViolation<T>> doValidateResource(T resource) {
        return validatorFactory.getValidator().validate(resource);
    }
    private <T> List<String> getValidationErrorMessages(Set<ConstraintViolation<T>> violations) {
        return violations.stream().map(ConstraintViolation::getMessage).toList();
    }


    @Nested
    class CreateRelyingPartyResourceValidationTests {

        @Test
        void testOrgnoConstraint() {
            CreateRelyingPartyResource resource = generateCreateRelyingPartyResource();
            resource.setOrgNr(generateInvalidOrgno());

            Set<ConstraintViolation<CreateRelyingPartyResource>> violations =
                doValidateResource(resource);
            List<String> validationErrorMessages = getValidationErrorMessages(violations);

            assertAll(
                () -> assertEquals(1, violations.size()),
                () -> assertTrue(validationErrorMessages.contains("invalid_orgnr"))
            );
        }

        @Test
        void testNullOrgnoDoesNotGiveOrgnoViolation() {
            CreateRelyingPartyResource resource = generateCreateRelyingPartyResource();
            resource.setOrgNr(null);

            Set<ConstraintViolation<CreateRelyingPartyResource>> violations =
                doValidateResource(resource);
            List<String> validationErrorMessages = getValidationErrorMessages(violations);

            assertAll(
                () -> assertEquals(1, violations.size()),
                () -> assertTrue(validationErrorMessages.contains("null_orgno")),
                () -> assertFalse(validationErrorMessages.contains("invalid_orgnr"))
            );
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "", // empty name
            "        ", // blank name
            "Dig$dir" // invalid symbols
        })
        void testNameConstraint(String invalidName) {
            CreateRelyingPartyResource resource = generateCreateRelyingPartyResource();
            resource.setName(invalidName);

            Set<ConstraintViolation<CreateRelyingPartyResource>> violations =
                doValidateResource(resource);
            List<String> validationErrorMessages = getValidationErrorMessages(violations);
            assertAll(
                () -> assertEquals(1, violations.size()),
                () -> assertTrue(validationErrorMessages.contains("invalid_name"))
            );
        }
    }

    @Test
    void testRelyingPartyResourceEaaConstraint() {
        RelyingPartyEaaResource invalidEaaResource = generateRelyingPartyEaaResource();
        invalidEaaResource.setNamespace("");

        RelyingPartyResource resource = generateRelyingPartyResource();
        resource.setRelyingPartyEaas(List.of(invalidEaaResource));

        Set<ConstraintViolation<RelyingPartyResource>> violations =
            doValidateResource(resource);
        List<String> validationErrorMessages = getValidationErrorMessages(violations);

        assertAll(
            () -> assertEquals(1, violations.size()),
            () -> assertTrue(validationErrorMessages.contains("blank_namespace"))
        );
    }


    @Test
    void testRelyingPartyResourceEntitlementConstraint() {
        List<RelyingPartyEntitlementResource> entitlementResources =
            List.of(
                generateRelyingPartyEntitlementResource(),
                generateRelyingPartyEntitlementResource(),
                generateRelyingPartyEntitlementResource()
            );
        entitlementResources.get(0).setEntitlement("");
        entitlementResources.get(2).setEntitlement("<script>bad!</script>");

        RelyingPartyResource resource = generateRelyingPartyResource();
        resource.setRelyingPartyEntitlements(entitlementResources);

        Set<ConstraintViolation<RelyingPartyResource>> violations =
            doValidateResource(resource);
        List<String> validationErrorMessages = getValidationErrorMessages(violations);

        assertAll(
            () -> assertEquals(2, violations.size()),
            () -> assertTrue(validationErrorMessages.contains("blank_entitlement")),
            () -> assertTrue(validationErrorMessages.contains("unsane_entitlement"))
        );
    }
}
