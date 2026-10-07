package no.eudiw.rp.register.exception;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class ApiExceptionHandlerResolverTest {

    @ParameterizedTest
    @MethodSource("wrappedExceptions")
    void preservesAdvicePriorityThroughSpringResolver(
        Exception exception, int expectedStatus, String expectedError, String expectedDescription
    ) throws Exception {
        var mockMvc = standaloneSetup(new FailingController(exception))
            .setControllerAdvice(
                new RegisterServiceExceptionHandler(), new ApiViolationExceptionHandler(), new AppExceptionHandler())
            .build();

        mockMvc.perform(get("/failure"))
            .andExpect(status().is(expectedStatus))
            .andExpect(jsonPath("$.error").value(expectedError))
            .andExpect(jsonPath("$.error_description").value(expectedDescription));
    }

    private static Stream<Arguments> wrappedExceptions() {
        return Stream.of(
            arguments(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "outer",
                    new NotFoundException("inner missing")),
                404, "not_found", "inner missing"),
            arguments(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "outer",
                    new ConstraintViolationException("invalid", Set.of())),
                400, "invalid_request", "Resource contains invalid field value(s)"),
            arguments(new ResponseStatusException(HttpStatus.BAD_REQUEST, "outer"),
                400, "invalid_request", "outer")
        );
    }

    @TestComponent
    @RestController
    static class FailingController {
        private final Exception failure;

        FailingController(Exception failure) {
            this.failure = failure;
        }

        @GetMapping("/failure")
        public void fail() throws Exception {
            throw failure;
        }
    }
}
