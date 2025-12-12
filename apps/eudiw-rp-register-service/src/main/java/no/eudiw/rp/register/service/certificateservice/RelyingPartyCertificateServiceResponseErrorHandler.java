package no.eudiw.rp.register.service.certificateservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.eudiw.rp.register.service.exception.ErrorResponseException;
import no.eudiw.rp.register.service.exception.UnauthorizedRequestException;
import no.eudiw.rp.register.service.exception.UnrecognizedErrorResponseException;
import no.eudiw.rp.register.api.resource.ErrorResponseResource;
import no.eudiw.rp.register.exception.RegisterServiceException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;
import java.net.URI;

public class RelyingPartyCertificateServiceResponseErrorHandler
    implements ResponseErrorHandler {
    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
        return !response.getStatusCode().is2xxSuccessful();
    }

    @Override
    public void handleError(@NonNull URI _requestUri,
                            @NonNull HttpMethod _method,
                            @NonNull ClientHttpResponse response)
        throws RegisterServiceException {

        try {
            if (response.getStatusCode().isSameCodeAs(HttpStatusCode.valueOf(401))) {
                throw new UnauthorizedRequestException();
            }
            if (!response.getStatusCode().isError()) {
                throw new UnrecognizedErrorResponseException(
                    "Unexpected HTTP status code in CA service non-success response");
            }
            ErrorResponseResource errorResource =
                new ObjectMapper().readValue(
                    response.getBody(), ErrorResponseResource.class);
            String errorMsg =
                "Bad request. CA service error response: " + errorResource.error();
            throw new ErrorResponseException(errorMsg);
        } catch (IOException e) {
            throw new UnrecognizedErrorResponseException("Unrecognized error response from CA service");
        }
    }
}
