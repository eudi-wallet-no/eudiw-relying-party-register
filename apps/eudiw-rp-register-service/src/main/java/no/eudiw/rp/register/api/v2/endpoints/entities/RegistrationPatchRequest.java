package no.eudiw.rp.register.api.v2.endpoints.entities;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.ConstraintViolation;
import tools.jackson.databind.node.ObjectNode;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

@Schema(description = "Partial registration update. Omitted sections and properties are unchanged; "
    + "supplied properties replace the current value, also when null.")
public record RegistrationPatchRequest(
    @Schema(implementation = RelyingPartyRequest.class, requiredMode = Schema.RequiredMode.NOT_REQUIRED,
        description = "Properties from PUT /v2/parties/{id}")
    ObjectNode relyingParty,
    @Schema(implementation = RelyingPartyServiceRequest.class, requiredMode = Schema.RequiredMode.NOT_REQUIRED,
        description = "Properties from PUT /v2/services/{id}")
    ObjectNode relyingPartyService,
    @Schema(implementation = RelyingPartyInstanceRequest.class, requiredMode = Schema.RequiredMode.NOT_REQUIRED,
        description = "Properties from PUT /v2/instances/{id}")
    ObjectNode relyingPartyInstance
) {
    private static final Map<String, Set<String>> PROPERTIES_BY_CONSTRAINED_NAME = Map.of(
        "supportUri", Set.of("supportURI"),
        "validSupportUri", Set.of("supportURI"),
        "validInfoUris", Set.of("infoURI"),
        "contactInformation", Set.of("supportURI", "email", "phone"),
        "validIntermediaryConfiguration", Set.of("isIntermediary", "usesIntermediaries", "servedWRPServices")
    );

    /**
     * Returns whether a violation concerns a supplied property. Constraints on unknown names always apply.
     */
    public static boolean concernsSuppliedProperty(
        ConstraintViolation<?> violation,
        Collection<String> suppliedProperties,
        Collection<String> requestProperties
    ) {
        String constrainedName = violation.getPropertyPath().iterator().next().getName();
        Set<String> constrainedProperties = PROPERTIES_BY_CONSTRAINED_NAME.get(constrainedName);
        if (constrainedProperties != null) {
            return constrainedProperties.stream().anyMatch(suppliedProperties::contains);
        }

        boolean isUnknownConstraint = !requestProperties.contains(constrainedName);
        return isUnknownConstraint || suppliedProperties.contains(constrainedName);
    }
}
