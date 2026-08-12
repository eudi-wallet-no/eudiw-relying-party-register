package no.idporten.eudiw.trustlist.domain.etsi602;

import jakarta.validation.Valid;
import org.jspecify.annotations.NonNull;

public record TrustedEntityService(@Valid @NonNull ServiceInformation serviceInformation) {
}
