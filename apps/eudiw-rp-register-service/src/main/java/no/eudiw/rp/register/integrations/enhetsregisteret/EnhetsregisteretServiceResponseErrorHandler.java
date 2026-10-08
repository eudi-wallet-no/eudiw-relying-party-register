package no.eudiw.rp.register.integrations.enhetsregisteret;

import no.eudiw.rp.register.exception.BadRequestException;
import no.eudiw.rp.register.exception.ErrorResponseException;
import no.eudiw.rp.register.exception.NotFoundException;
import no.eudiw.rp.register.exception.UnrecognizedErrorResponseException;
import no.eudiw.rp.register.exception.RegisterServiceException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;
import java.net.URI;

public class EnhetsregisteretServiceResponseErrorHandler
    implements ResponseErrorHandler {

    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
        return !response.getStatusCode().isSameCodeAs(HttpStatus.OK);
    }

    @Override
    public void handleError(@NonNull URI _requestUrl,
                            @NonNull HttpMethod _method,
                            @NonNull ClientHttpResponse response)
        throws RegisterServiceException {
        try {
            HttpStatusCode statusCode = response.getStatusCode();
            if (statusCode.isSameCodeAs(HttpStatus.NOT_FOUND)
                || statusCode.isSameCodeAs(HttpStatus.GONE)) {
                throw new NotFoundException("No such registration in Enhetsregisteret");
            }
            if (statusCode.is5xxServerError()) {
                throw new ErrorResponseException("Enhetsregisteret server error");
            }
            if (statusCode.is4xxClientError()) {
                // external validation should prevent this from being reached.
                throw new BadRequestException("Invalid organization number");
            }
        } catch (IOException e) {
            throw new UnrecognizedErrorResponseException(
                "Failed to parse Enhetsregisteret response", e);
        }
        throw new UnrecognizedErrorResponseException(
            "Unrecognized non-success response from Enhetsregisteret");
    }
}
