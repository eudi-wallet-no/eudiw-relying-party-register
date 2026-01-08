package no.idporten.eudiw.credential.registry.exception;


import com.fasterxml.jackson.annotation.JsonProperty;
import io.opentelemetry.api.trace.Span;
import jakarta.validation.constraints.NotNull;

public record ErrorResponse(
        @NotNull
        @JsonProperty("error")
        String error,
        @NotNull
        @JsonProperty("error_description")
        String errorDescription
) {

    public ErrorResponse(@NotNull
                         @JsonProperty("error")
                         String error, @NotNull
                         @JsonProperty("error_description")
                         String errorDescription) {
        this.error = error;
        this.errorDescription = String.format("%s (%s)", errorDescription, Span.current().getSpanContext().getTraceId());
    }

}