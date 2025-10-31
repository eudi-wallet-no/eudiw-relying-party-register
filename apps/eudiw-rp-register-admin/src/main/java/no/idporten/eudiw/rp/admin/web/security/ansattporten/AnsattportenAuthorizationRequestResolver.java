package no.idporten.eudiw.rp.admin.web.security.ansattporten;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.idporten.eudiw.rp.admin.web.security.OAuth2Constants;
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
        Map<String, Object> attributes =
            new HashMap<>(authorizationRequest.getAttributes());
        Map<String, Object> additionalParameters =
            new HashMap<>(authorizationRequest.getAdditionalParameters());

        addPkce(attributes, additionalParameters);

        if (!ansattportenProperties.allowSyntheticReportee()
                || !"true".equals(httpRequest.getParameter("use_synthetic_reportee"))) {
            addAuthorizationDetails(attributes, additionalParameters);
        }

        return OAuth2AuthorizationRequest.from(authorizationRequest)
                                         .attributes(attributes)
                                         .additionalParameters(additionalParameters)
                                         .build();
    }

    @SneakyThrows
    private void addAuthorizationDetails(Map<String, Object> attributes,
                                         Map<String, Object> additionalParameters) {
        attributes.put(
            OAuth2Constants.OAUTH2_AUTHORIZATION_DETAILS_PARAMETER,
            ansattportenProperties.authorizationDetails());

        String authDetailsListString =
            new ObjectMapper().writeValueAsString(
                List.of(ansattportenProperties.authorizationDetails()));
        additionalParameters.put(
            OAuth2Constants.OAUTH2_AUTHORIZATION_DETAILS_PARAMETER, authDetailsListString);
    }

    private void addPkce(Map<String, Object> attributes,
                         Map<String, Object> additionalParameters) {
        String codeVerifier = this.secureKeyGenerator.generateKey();
        attributes.put(OAuth2Constants.OAUTH2_CODE_VERIFIER_PARAMETER, codeVerifier);
        try {
            String codeChallenge = sha256Hash(codeVerifier);
            additionalParameters.put(OAuth2Constants.OAUTH2_CODE_CHALLENGE_PARAMETER, codeChallenge);
            additionalParameters.put(OAuth2Constants.OAUTH2_CODE_CHALLENGE_METHOD_PARAMETER, "S256");
        } catch (NoSuchAlgorithmException _) {
            additionalParameters.put(OAuth2Constants.OAUTH2_CODE_CHALLENGE_PARAMETER, codeVerifier);
        }
    }

    private static String sha256Hash(String s) throws NoSuchAlgorithmException {
        byte[] sha256Digest =
            MessageDigest.getInstance("SHA-256")
                         .digest(s.getBytes(StandardCharsets.US_ASCII));
        return Base64.getUrlEncoder()
                     .withoutPadding()
                     .encodeToString(sha256Digest);
    }
}
