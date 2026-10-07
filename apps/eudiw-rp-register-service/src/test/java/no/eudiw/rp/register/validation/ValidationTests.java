package no.eudiw.rp.register.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ValidatorFactory;

import static no.eudiw.rp.register.testdata.ResourceGenerator.*;

import no.eudiw.rp.register.api.v1.resource.relyingparty.CreateRelyingPartyResource;
import no.eudiw.rp.register.api.v1.resource.relyingparty.RelyingPartyEaaResource;
import no.eudiw.rp.register.api.v1.resource.relyingparty.RelyingPartyResource;
import no.eudiw.rp.register.api.v1.resource.relyingparty.SearchRelyingPartyResource;
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
@ActiveProfiles("junit")
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
            CreateRelyingPartyResource resource =
                generateCreateRelyingPartyResource().withOrgNr(generateInvalidOrgno());

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
            CreateRelyingPartyResource resource =
                generateCreateRelyingPartyResource().withOrgNr(null);

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
        @ValueSource(strings = {"", "     "})
        void testNotBlankNameConstraint(String blankName) {
            CreateRelyingPartyResource resourceWithInvalidName =
                generateCreateRelyingPartyResource()
                    .withTradeName(blankName);

            Set<ConstraintViolation<CreateRelyingPartyResource>> violations =
                doValidateResource(resourceWithInvalidName);
            List<String> validationErrorMessages = getValidationErrorMessages(violations);
            assertAll(
                () -> assertEquals(1, violations.size()),
                () -> assertTrue(validationErrorMessages.contains("blank_name"))
            );

        }
    }

    @Nested
    class RelyingPartyResourceTests {
        @Test
        void testRelyingPartyResourceEaaConstraint() {
            RelyingPartyEaaResource invalidEaaResource =
                new RelyingPartyEaaResource("   ", "valid-intent");

            RelyingPartyResource resource =
                generateRelyingPartyResource()
                    .withRelyingPartyEaas(List.of(invalidEaaResource));

            Set<ConstraintViolation<RelyingPartyResource>> violations =
                doValidateResource(resource);
            List<String> validationErrorMessages = getValidationErrorMessages(violations);

            assertAll(
                () -> assertEquals(1, violations.size()),
                () -> assertTrue(validationErrorMessages.contains("blank_namespace"))
            );
        }
    }

    @Nested
    class SaneStringConstraintTests {

        @Test
        @DisplayName("then EAA namespace with a double quote is rejected")
        void testEaaNamespaceRejectsDoubleQuote() {
            RelyingPartyEaaResource eaaResource =
                new RelyingPartyEaaResource("\"onerror", "test");

            Set<ConstraintViolation<RelyingPartyEaaResource>> violations =
                doValidateResource(eaaResource);

            assertAll(
                () -> assertEquals(1, violations.size()),
                () -> assertEquals("namespace", violations.iterator().next().getPropertyPath().toString())
            );
        }

        @Test
        @DisplayName("then EAA namespace with allowed characters is accepted")
        void testEaaNamespaceAcceptsAllowedCharacters() {
            RelyingPartyEaaResource eaaResource =
                new RelyingPartyEaaResource("no:minid:mpid:1", "test");

            Set<ConstraintViolation<RelyingPartyEaaResource>> violations =
                doValidateResource(eaaResource);

            assertTrue(violations.isEmpty());
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "digæøåÅØÆdir", // norwegian letters
            "dig0123456789dir", // numbers
            "dig.,-:'&/ dir", // special symbols
            "digaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                + "aadir" // length 255 string
        })
        void testValidEdgeCaseInputStrings(String str) {
            SearchRelyingPartyResource searchResource =
                new SearchRelyingPartyResource(str);
            Set<ConstraintViolation<SearchRelyingPartyResource>> violations =
                doValidateResource(searchResource);
            assertTrue(violations.isEmpty());
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "digöÄdir", // non-norwegian letters
            "dig!?dir", // disallowed symbols
            "dig\"onerror", // double quote
            "dig\tdir", // disallowed whitespace
            "digaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                + "aaadir" // string too long (256 characters)
        })
        void testInvalidEdgeCaseInputStrings(String str) {
            SearchRelyingPartyResource searchResource =
                new SearchRelyingPartyResource(str);
            Set<ConstraintViolation<SearchRelyingPartyResource>> violations =
                doValidateResource(searchResource);

            List<String> validationErrorMessages = getValidationErrorMessages(violations);

            assertAll(
                () -> assertEquals(1, violations.size()),
                () -> assertTrue(validationErrorMessages.contains("unsane_string"))
            );
        }
    }
}




