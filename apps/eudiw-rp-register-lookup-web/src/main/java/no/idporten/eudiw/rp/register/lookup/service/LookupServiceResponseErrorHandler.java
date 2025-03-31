package no.idporten.eudiw.rp.register.lookup.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.idporten.eudiw.rp.register.lookup.service.exception.ErrorResponseException;
import no.idporten.eudiw.rp.register.lookup.service.exception.UnauthorizedUserRequestException;
import no.idporten.eudiw.rp.register.lookup.service.exception.UnrecognizedErrorResponseException;
import no.idporten.eudiw.rp.register.lookup.web.resource.ErrorResource;
import no.idporten.eudiw.rp.register.lookup.service.exception.LookupServiceException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;
import java.net.URI;

public class LookupServiceResponseErrorHandler
    implements ResponseErrorHandler {

    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
        return !response.getStatusCode().is2xxSuccessful();
    }

    @Override
    public void handleError(@NonNull URI _requestUrl,
                            @NonNull HttpMethod _method,
                            @NonNull ClientHttpResponse response)
        throws LookupServiceException {

        try {
            if (response.getStatusCode().isSameCodeAs(HttpStatusCode.valueOf(401))) {
                throw new UnauthorizedUserRequestException();
            }
            if (!response.getStatusCode().isError()) {
                throw new UnrecognizedErrorResponseException(
                    "Unexpected HTTP status code in register service response");
            }
            ErrorResource errorResource =
                new ObjectMapper().readValue(
                    response.getBody(), ErrorResource.class);
            String errorMsg =
                "Bad request. Register service error response: " + errorResource.error();
            throw new ErrorResponseException(errorMsg);
        } catch (IOException e) {
            throw new UnrecognizedErrorResponseException(
                "Unrecognized error response from register service", e);
        }
    }
}
