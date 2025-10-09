package no.idporten.eudiw.rp.admin.service.enhetsregisteretservice;

import no.idporten.eudiw.rp.admin.exception.AdminServiceException;
import no.idporten.eudiw.rp.admin.service.exception.*;
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
        throws AdminServiceException {
        try {
            HttpStatusCode statusCode = response.getStatusCode();
            if (statusCode.isSameCodeAs(HttpStatus.NOT_FOUND)
                || statusCode.isSameCodeAs(HttpStatus.GONE)) {
                throw new NotFoundException("No such registration in Enhetsregisteret");
            }
            if (statusCode.is5xxServerError()) {
                throw new ErrorResponseException("server_error", "Enhetsregisteret server error");
            }
            if (statusCode.is4xxClientError()) {
                // external validation should prevent this from being reached.
                throw new BadRequestException("Invalid organization number");
            }
        } catch (IOException _) { }
        throw new UnrecognizedErrorResponseException(
            "Unrecognized non-success response from Enhetsregisteret");
    }
}
