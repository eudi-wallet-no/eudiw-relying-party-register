package no.idporten.eudiw.trustlist.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.opentelemetry.api.trace.Span;


@JsonInclude
public record ErrorResponse(
    @JsonProperty("error")
    String error,
    @JsonProperty("error_description")
    String errorDescription
) {

    public ErrorResponse(@JsonProperty("error") String error, @JsonProperty("error_description") String errorDescription) {
        this.error = error;
        this.errorDescription = String.format("%s (%s)", errorDescription, Span.current().getSpanContext().getTraceId());
    }

}
