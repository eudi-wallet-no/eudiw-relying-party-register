package no.idporten.eudiw.rp.register.lookup.service.credentialsservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.idporten.eudiw.rp.register.lookup.exception.LookupServiceException;
import no.idporten.eudiw.rp.register.lookup.service.exception.BadRequestException;
import no.idporten.eudiw.rp.register.lookup.service.exception.ErrorResponseException;
import no.idporten.eudiw.rp.register.lookup.service.exception.UnrecognizedErrorResponseException;
import no.idporten.eudiw.rp.register.lookup.web.resource.ErrorResponseResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;
import java.net.URI;

public class CredentialsServiceResponseErrorHandler
    implements ResponseErrorHandler {

    private static final ObjectMapper objectMapper = new ObjectMapper();

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
            HttpStatusCode statusCode = response.getStatusCode();
            if (!statusCode.isError()) {
                throw new UnrecognizedErrorResponseException(
                    "Unexpected HTTP status code (%s) in credential registry response"
                        .formatted(statusCode));
            }
            if (statusCode.is4xxClientError()) {
                throw new BadRequestException("Bad request to credential registry");
            }
            if (statusCode.is5xxServerError()) {
                throw new ErrorResponseException(
                    "server_error", "5xx error response from credential registry");
            }
            ErrorResponseResource errorResource =
                objectMapper.readValue(response.getBody(), ErrorResponseResource.class);
            throw ErrorResponseException.fromResource(errorResource);
        } catch (IOException e) {
            throw new UnrecognizedErrorResponseException(
                "Failed to parse error response from credential registry", e);
        }
    }
}
