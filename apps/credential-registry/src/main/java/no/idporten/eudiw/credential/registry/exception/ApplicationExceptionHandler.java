package no.idporten.eudiw.credential.registry.exception;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApplicationExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApplicationExceptionHandler.class);

    @ExceptionHandler(CredentialRegisterException.class)
    public ResponseEntity<ErrorResponse> credentialRegisterException(CredentialRegisterException credentialRegisterException) {
        log.warn("Credential Registry exception ", credentialRegisterException);
        return ResponseEntity
                .status(credentialRegisterException.getHttpStatus())
                .body(new ErrorResponse(credentialRegisterException.getError(), credentialRegisterException.getErrorDescription()));
    }
}