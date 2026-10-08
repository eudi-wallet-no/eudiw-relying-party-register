package no.eudiw.rp.register.integrations.enhetsregisteret;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import no.eudiw.rp.register.exception.NotFoundException;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
public class EnhetsregisteretService {

    private final RestClient enhetsregisteretRestClient;
    private final List<String> knownPublicSectorCodes;
    private final Validator validator;

    public EnhetsregisteretResponse queryOrgno(String orgno) {
        EnhetsregisteretRawResponse response =
            enhetsregisteretRestClient.get()
                .uri("/{orgno}", orgno)
                .retrieve()
                .toEntity(EnhetsregisteretRawResponse.class)
                .getBody();
        Objects.requireNonNull(response); // error handler should fire before this one.

        var validationErrors = validator.validate(response);
        if (!validationErrors.isEmpty()) {
            throw new NotFoundException("Invalid Enhetsregisteret response for orgno=%s".formatted(orgno));
        }

        boolean publicSector =
            response.sectorCodes()
                    .values()
                    .stream()
                    .anyMatch(knownPublicSectorCodes::contains);
        return new EnhetsregisteretResponse(response.name(), publicSector);
    }

    public record EnhetsregisteretResponse(
        String name,
        boolean publicSector
    ) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record EnhetsregisteretRawResponse(
        @Valid
        @NotNull
        @JsonProperty("institusjonellSektorkode")
        SectorCodes sectorCodes,
        @NotBlank
        @JsonProperty("navn")
        String name
    ) {
        // brreg API specifies institusjonellSektorKode.kode as a comma-separated
        // string of 1 or more 4-digit sector codes.
        @JsonIgnoreProperties(ignoreUnknown = true)
        private record SectorCodes(
            @NotEmpty
            List<@Pattern(regexp = "\\d{4}") String> values
        ) {
            @JsonCreator
            public SectorCodes(@JsonProperty("kode") String commaSeparatedSectorCodes) {
                this(Arrays.stream(commaSeparatedSectorCodes.split(","))
                           .map(String::strip)
                           .toList());
            }
        }
    }
}
