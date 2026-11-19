package no.eudiw.rp.register.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ValidatorFactory;

import static no.eudiw.rp.register.testdata.TestDataGenerator.generateValidOrgno;

import no.eudiw.rp.register.api.v2.endpoints.entities.CreateInstanceRequest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
    class SaneStringConstraintTests {

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
            CreateInstanceRequest request = new CreateInstanceRequest(
                generateValidOrgno(), str, List.of(new CreateInstanceRequest.EntitlementRequest("urn:test:role")));
            Set<ConstraintViolation<CreateInstanceRequest>> violations =
                doValidateResource(request);
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
            CreateInstanceRequest request = new CreateInstanceRequest(
                generateValidOrgno(), str, List.of(new CreateInstanceRequest.EntitlementRequest("urn:test:role")));
            Set<ConstraintViolation<CreateInstanceRequest>> violations =
                doValidateResource(request);

            List<String> validationErrorMessages = getValidationErrorMessages(violations);

            assertAll(
                () -> assertEquals(1, violations.size()),
                () -> assertTrue(validationErrorMessages.contains("unsane_string"))
            );
        }
    }
}
