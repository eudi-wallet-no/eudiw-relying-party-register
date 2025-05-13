package no.idporten.eudiw.rp.admin.service;

import no.idporten.eudiw.rp.admin.service.exception.AdminServiceException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;
import java.net.URI;

public class RPResponseErrorHandler
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
            if (response.getStatusCode().isSameCodeAs(HttpStatusCode.valueOf(400))) {
                throw new AdminServiceException("Bad request");
            }
            if (response.getStatusCode().isSameCodeAs(HttpStatusCode.valueOf(401))) {
                throw new AdminServiceException("Unauthorized access");
            }
            if (!response.getStatusCode().isError()) {
                throw new AdminServiceException(
                    "Unexpected HTTP status code in register service response");
            }
        } catch (Exception e) {
            throw new AdminServiceException(
                "Unrecognized error response from register service", e);
        }
    }
}
