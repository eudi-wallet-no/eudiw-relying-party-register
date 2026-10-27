package no.idporten.eudiw.rp.admin.integrations;

import no.idporten.eudiw.rp.admin.exception.AdminServiceException;
import no.idporten.eudiw.rp.admin.service.exception.UnrecognizedErrorResponseException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.function.Supplier;

public final class RestClientCall {

    private RestClientCall() {
    }

    public static <T> T execute(String service, String operation, Supplier<T> request) {
        try {
            return request.get();
        } catch (ResourceAccessException e) {
            throw new AdminServiceException("Unable to contact " + service + " to " + operation, e);
        } catch (RestClientResponseException e) {
            throw e;
        } catch (RestClientException e) {
            throw new UnrecognizedErrorResponseException("Unable to read response from " + service + " to " + operation, e);
        }
    }
}
