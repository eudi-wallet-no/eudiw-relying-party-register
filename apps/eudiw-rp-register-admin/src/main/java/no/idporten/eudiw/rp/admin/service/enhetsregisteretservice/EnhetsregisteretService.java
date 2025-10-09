package no.idporten.eudiw.rp.admin.service.enhetsregisteretservice;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
public class EnhetsregisteretService {

    private static final List<String> KNOWN_PUBLIC_SECTOR_CODES = List.of(
        "6100", // "Statsforvaltningen"
        "6500"  // "Kommuneforvaltningen"
    );

    private final RestClient enhetsregisteretRestClient;

    public boolean getPublicSectorForOrgno(String orgno) {
        EnhetsregisteretResponse response =
            enhetsregisteretRestClient.get()
                .uri("/{orgno}", orgno)
                .retrieve()
                .toEntity(EnhetsregisteretResponse.class)
                .getBody();
        return Objects.requireNonNull(response) // error handler should fire before this one.
                      .sectorCodes()
                      .values()
                      .stream()
                      .anyMatch(KNOWN_PUBLIC_SECTOR_CODES::contains);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EnhetsregisteretResponse(
        @Valid
        @NotNull
        @JsonProperty("institusjonellSektorkode")
        SectorCodes sectorCodes
    ) {
        // brreg API specifies institusjonellSektorKode.kode as a comma-separated
        // string of 1 or more 4-digit sector codes.
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record SectorCodes(
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
