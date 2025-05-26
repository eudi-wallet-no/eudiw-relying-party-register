package no.idporten.eudiw.rp.admin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.idporten.eudiw.rp.admin.exception.AdminServiceException;
import no.idporten.eudiw.rp.admin.service.exception.ErrorResponseException;
import no.idporten.eudiw.rp.admin.service.exception.UnauthorizedRequestException;
import no.idporten.eudiw.rp.admin.service.exception.UnrecognizedErrorResponseException;
import no.idporten.eudiw.rp.admin.web.resource.ErrorResponseResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.client.ResponseErrorHandler;

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
            if (response.getStatusCode().isSameCodeAs(HttpStatusCode.valueOf(401))) {
                throw new UnauthorizedRequestException();
            }
            if (!response.getStatusCode().isError()) {
                System.out.println("no error");
                throw new UnrecognizedErrorResponseException(
                    "Unexpected HTTP status code in register service response");
            }
            ErrorResponseResource errorResource =
                new ObjectMapper().readValue(
                    response.getBody(), ErrorResponseResource.class);
            String errorMsg = "Bad request. Register service error response: "
                                  + errorResource.error();
            throw new ErrorResponseException(errorMsg);
        } catch (IOException e) {
            System.out.println("something happend: " + e);
            throw new UnrecognizedErrorResponseException(
                "Unrecognized error response from register service");
        }
    }
}
