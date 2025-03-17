package no.eudiw.rp.register.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice("no.eudiw.rp.register.api")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException e) {
        return ResponseEntity
                .status(e.getHttpStatus())
                .body(ErrorResponse.builder()
                        .error(e.getError())
                        .errorDescription(e.getErrorDescription())
                        .build());
    }

    @ExceptionHandler(RelyingPartyRegisterServiceException.class)
    public ResponseEntity<ErrorResponse> handleApiException(RelyingPartyRegisterServiceException e) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.builder()
                        .error(e.getMessage())
                        .errorDescription(e.getLocalizedMessage())
                        .build());
    }
}
