package no.idporten.eudiw.rp.admin.service;

import no.idporten.eudiw.rp.admin.exception.AdminServiceException;
import no.idporten.eudiw.rp.admin.service.exception.*;
import no.idporten.eudiw.rp.admin.web.resource.ErrorResponseResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.client.ResponseErrorHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;

public class RelyingPartiesServiceResponseErrorHandler
    implements ResponseErrorHandler {

    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
        return !response.getStatusCode().is2xxSuccessful();
    }

    @Override
    public void handleError(@NonNull URI _requestUrl,
                            @NonNull HttpMethod _method,
                            @NonNull ClientHttpResponse response)
        throws AdminServiceException {

        try {
            HttpStatusCode statusCode = response.getStatusCode();
            if (!statusCode.isError()) {
                throw new UnrecognizedErrorResponseException(
                    "Unexpected HTTP status code (%s) in register service response"
                        .formatted(statusCode));
            }
            if (statusCode.isSameCodeAs(HttpStatus.UNAUTHORIZED)) {
                throw new UnauthorizedRequestException();
            }
            if (statusCode.isSameCodeAs(HttpStatus.GONE)) {
                throw new NotFoundException("Requested RP is deleted");
            }
            ErrorResponseResource errorResource =
                new JsonMapper().readValue(
                    response.getBody(), ErrorResponseResource.class);
            throw ErrorResponseException.fromResource(errorResource);
        } catch (IOException | JacksonException e) {
            throw new UnrecognizedErrorResponseException(
                "Unrecognized error response from register service");
        }
    }
}
