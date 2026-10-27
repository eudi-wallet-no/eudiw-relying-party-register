package no.idporten.eudiw.rp.admin.integrations;

import no.idporten.eudiw.rp.admin.exception.AdminServiceException;
import no.idporten.eudiw.rp.admin.service.exception.UnrecognizedErrorResponseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RestClientCallTests {

    @Test
    @DisplayName("Should report the failed service and operation when the server cannot be reached")
    void shouldReportFailedServiceAndOperationWhenServerCannotBeReached() {
        var connectionFailure = new ResourceAccessException("Connection refused");

        var failure = assertThrows(AdminServiceException.class,
            () -> RestClientCall.execute("register service", "retrieve entitlements", () -> {
                throw connectionFailure;
            }));

        assertEquals("Unable to contact register service to retrieve entitlements", failure.getMessage());
        assertSame(connectionFailure, failure.getCause());
    }

    @Test
    @DisplayName("Should distinguish an unreadable response from a connection failure")
    void shouldDistinguishUnreadableResponseFromConnectionFailure() {
        var readFailure = new RestClientException("Invalid response");

        var failure = assertThrows(UnrecognizedErrorResponseException.class,
            () -> RestClientCall.execute("register service", "search instances", () -> {
                throw readFailure;
            }));

        assertEquals("Unable to read response from register service to search instances", failure.getMessage());
        assertSame(readFailure, failure.getCause());
    }

    @Test
    @DisplayName("Should preserve an HTTP error rather than report an unreadable response")
    void shouldPreserveHttpErrorRatherThanReportUnreadableResponse() {
        var serverFailure = new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE);

        var failure = assertThrows(HttpServerErrorException.class,
            () -> RestClientCall.execute("register service", "retrieve registrations", () -> {
                throw serverFailure;
            }));

        assertSame(serverFailure, failure);
    }
}
