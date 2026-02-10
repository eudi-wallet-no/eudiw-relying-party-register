package no.idporten.eudiw.rp.admin.web.security.ansattporten.authzdetails;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AuthorizationDetailsMapper {

    public static final String TYPE_KEY = "type";

    private final JsonMapper jsonMapper;
    private final Validator validator;

    public AuthorizationDetails.Request asRequest(Map<String, Object> map) {
        return (AuthorizationDetails.Request) fromMap(map, false);
    }

    public List<AuthorizationDetails.Request> asRequests(List<Map<String, Object>> maps) {
        return maps.stream().map(this::asRequest).toList();
    }

    public AuthorizationDetails.Response asResponse(Map<String, Object> map) {
        return (AuthorizationDetails.Response) fromMap(map, true);
    }

    private AuthorizationDetails fromMap(Map<String, Object> map, boolean doMapToResponse) {
        Class<? extends AuthorizationDetails> targetClass = getTargetClass(map, doMapToResponse);
        AuthorizationDetails authorizationDetails = jsonMapper.convertValue(map, targetClass);
        return doValidate(authorizationDetails);
    }

    private Class<? extends AuthorizationDetails> getTargetClass(
        Map<String, Object> source, boolean doMapToResponse) {
        if (!source.containsKey(TYPE_KEY)) {
            throw new InvalidAuthorizationDetailsException(
                "authorization_details missing \"type\" key");
        }

        String type = source.get(TYPE_KEY).toString();
        return switch (type) {
            case AltinnServiceDetails.TYPE_VALUE ->
                doMapToResponse ? AltinnServiceDetails.Response.class
                                : AltinnServiceDetails.Request.class;
            case OrgnoDetails.TYPE_VALUE ->
                doMapToResponse ? OrgnoDetails.Response.class
                                : OrgnoDetails.Request.class;
            default ->
                throw new InvalidAuthorizationDetailsException(
                    "Unrecognized or unsupported authorization_details type: %s"
                        .formatted(type));
        };
    }

    private AuthorizationDetails doValidate(AuthorizationDetails authorizationDetails) {
        Set<ConstraintViolation<AuthorizationDetails>> violations =
            validator.validate(authorizationDetails);
        if (!violations.isEmpty()) {
            String errorMsg = violations.stream()
                                        .map(ConstraintViolation::getMessage)
                                        .collect(Collectors.joining(","));
            throw new InvalidAuthorizationDetailsException(
                "Invalid authorization_details. Constraint violations: %s"
                    .formatted(errorMsg));
        }
        return authorizationDetails;
    }
}
