package no.eudiw.rp.register.exception;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestComponent;

import java.util.Set;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class ApiExceptionHandlerTest {

    @Test
    @DisplayName("A wrapped API violation retains its bad request response")
    void wrappedApiViolation() throws Exception {
        var mockMvc = standaloneSetup(new FailingController())
            .setControllerAdvice(
                new RegisterServiceExceptionHandler(), new ApiViolationExceptionHandler(), new AppExceptionHandler())
            .build();

        mockMvc.perform(get("/failure"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("invalid_request"))
            .andExpect(jsonPath("$.error_description").value("Resource contains invalid field value(s)"));
    }

    @TestComponent
    @RestController
    static class FailingController {
        @GetMapping("/failure")
        public void fail() {
            throw new RuntimeException(new ConstraintViolationException("invalid", Set.of()));
        }
    }
}
