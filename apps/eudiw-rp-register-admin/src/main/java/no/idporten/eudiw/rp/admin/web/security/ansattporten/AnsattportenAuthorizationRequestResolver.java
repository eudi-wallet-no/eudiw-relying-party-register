package no.idporten.eudiw.rp.admin.web.security.ansattporten;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.rp.admin.exception.AdminServiceException;
import no.idporten.eudiw.rp.admin.web.security.AuthConstants;
import no.idporten.eudiw.rp.admin.web.security.ansattporten.authzdetails.AuthorizationDetails;
import no.idporten.eudiw.rp.admin.web.security.ansattporten.authzdetails.InvalidAuthorizationDetailsException;
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@RequiredArgsConstructor
public class AnsattportenAuthorizationRequestResolver
    implements OAuth2AuthorizationRequestResolver {

    private final StringKeyGenerator secureKeyGenerator =
        new Base64StringKeyGenerator(Base64.getUrlEncoder().withoutPadding(), 96);

    private final OAuth2AuthorizationRequestResolver delegateResolver;
    private final AnsattportenProperties ansattportenProperties;
    private final ObjectMapper objectMapper;

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        OAuth2AuthorizationRequest authorizationRequest =
            delegateResolver.resolve(request);
        if (authorizationRequest == null) {
            return null;
        }
        return "ansattporten".equals(authorizationRequest.getAttributes().get("registration_id"))
            ? customizeAnsattportenAuthzRequest(request, authorizationRequest)
            : authorizationRequest;
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request,
                                              String clientRegistrationId) {
        OAuth2AuthorizationRequest authorizationRequest =
            delegateResolver.resolve(request, clientRegistrationId);
        if (authorizationRequest == null) {
            return null;
        }
        return "ansattporten".equals(clientRegistrationId)
                   ? customizeAnsattportenAuthzRequest(request, authorizationRequest)
                   : authorizationRequest;
    }

    private OAuth2AuthorizationRequest customizeAnsattportenAuthzRequest(
        HttpServletRequest httpRequest,
        OAuth2AuthorizationRequest authorizationRequest) {

        OAuth2AuthorizationRequest.Builder builder =
            OAuth2AuthorizationRequest.from(authorizationRequest);

        builder = addBaseAnsattportenAuthorizationParameters(builder);

        boolean entraIdRequested = "true".equals(httpRequest.getParameter("use_entra_id"));
        boolean syntheticReporteeRequested =
            "true".equals(httpRequest.getParameter("use_synthetic_reportee"));

        if (ansattportenProperties.isAllowEntraId() && entraIdRequested) {
            builder = addEntraIdParameters(builder);
        }
        else if (!ansattportenProperties.isAllowSyntheticReportee()
                     || !syntheticReporteeRequested) {
            builder = addAuthorizationDetails(builder);
        }

        return builder.build();
    }

    private OAuth2AuthorizationRequest.Builder addAuthorizationDetails(
        OAuth2AuthorizationRequest.Builder requestBuilder) {

        List<AuthorizationDetails.Request> authorizationDetails =
            ansattportenProperties.getRequestAuthorizationDetails();

        Map<String, Object> attributes = Map.of(
            AuthConstants.AUTHORIZATION_DETAILS_PARAMETER,
            authorizationDetails);

        Map<String, Object> additionalParams = Map.of(
            AuthConstants.AUTHORIZATION_DETAILS_PARAMETER,
            requestAuthorizationDetailsToJson(authorizationDetails));

        return requestBuilder.attributes(attributes)
                             .additionalParameters(additionalParams);
    }

    private OAuth2AuthorizationRequest.Builder addBaseAnsattportenAuthorizationParameters(
        OAuth2AuthorizationRequest.Builder requestBuilder) {

        Map<String, Object> attributes = new HashMap<>();
        attributes.put(AuthConstants.PROMPT_PARAMETER, AuthConstants.PROMPT_LOGIN_VALUE);

        Map<String, Object> additionalParams = new HashMap<>(attributes);

        try {
            String codeVerifier = this.secureKeyGenerator.generateKey();
            String codeChallenge = sha256Hash(codeVerifier);

            attributes.put(AuthConstants.CODE_VERIFIER_PARAMETER, codeVerifier);
            additionalParams.put(AuthConstants.CODE_CHALLENGE_PARAMETER, codeChallenge);
            additionalParams.put(AuthConstants.CODE_CHALLENGE_METHOD_PARAMETER, AuthConstants.CODE_CHALLENGE_METHOD_S256);
        } catch (NoSuchAlgorithmException e) {
            throw new AdminServiceException(
                "SHA-256 challenge required, but generation failed unexpectedly", e);
        }

        return requestBuilder.attributes(attributes)
                             .additionalParameters(additionalParams);
    }

    private OAuth2AuthorizationRequest.Builder addEntraIdParameters(
        OAuth2AuthorizationRequest.Builder requestBuilder) {
        List<AuthorizationDetails.Request> authorizationDetails =
            ansattportenProperties.getEntraIdRequestAuthorizationDetails();

        Map<String, Object> attributes = Map.of(
            AuthConstants.AUTHORIZATION_DETAILS_PARAMETER,
            authorizationDetails,
            AuthConstants.ACR_VALUES_PARAMETER,
            AuthConstants.ACR_ENTRAID_VALUE);

        Map<String, Object> additionalParams = Map.of(
            AuthConstants.AUTHORIZATION_DETAILS_PARAMETER,
            requestAuthorizationDetailsToJson(authorizationDetails),
            AuthConstants.ACR_VALUES_PARAMETER,
            AuthConstants.ACR_ENTRAID_VALUE);

        return requestBuilder.attributes(attributes)
                             .additionalParameters(additionalParams);
    }

    private static String sha256Hash(String s) throws NoSuchAlgorithmException {
        byte[] sha256Digest =
            MessageDigest.getInstance("SHA-256")
                         .digest(s.getBytes(StandardCharsets.US_ASCII));
        return Base64.getUrlEncoder()
                     .withoutPadding()
                     .encodeToString(sha256Digest);
    }

    private String requestAuthorizationDetailsToJson(
        List<AuthorizationDetails.Request> requestAuthorizationDetails) {
        try {
            return objectMapper.writeValueAsString(requestAuthorizationDetails);
        } catch (JsonProcessingException e) {
            throw new InvalidAuthorizationDetailsException(
                "Unexpected error in request authorization_details", e);
        }
    }
}
