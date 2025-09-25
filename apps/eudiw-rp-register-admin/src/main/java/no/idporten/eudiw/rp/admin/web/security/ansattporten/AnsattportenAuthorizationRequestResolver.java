package no.idporten.eudiw.rp.admin.web.security.ansattporten;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import no.idporten.eudiw.rp.admin.web.security.OAuth2Constants;
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class AnsattportenAuthorizationRequestResolver
    implements OAuth2AuthorizationRequestResolver {

    private final StringKeyGenerator secureKeyGenerator =
        new Base64StringKeyGenerator(Base64.getUrlEncoder().withoutPadding(), 96);

    private final OAuth2AuthorizationRequestResolver delegateResolver;
    private final AnsattportenAuthzConfig ansattportenAuthzConfig;

    public AnsattportenAuthorizationRequestResolver(
        ClientRegistrationRepository clientRegistrationRepository,
        AnsattportenAuthzConfig ansattportenAuthzConfig) {
        this.delegateResolver = new DefaultOAuth2AuthorizationRequestResolver(
            clientRegistrationRepository, "/oauth2/authorization");
        this.ansattportenAuthzConfig = ansattportenAuthzConfig;
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        OAuth2AuthorizationRequest authorizationRequest =
            delegateResolver.resolve(request);
        return authorizationRequest != null
                   ? customizeAuthorizationRequest(authorizationRequest)
                   : null;
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request,
                                              String clientRegistrationId) {
        OAuth2AuthorizationRequest authorizationRequest =
            delegateResolver.resolve(request, clientRegistrationId);
        return authorizationRequest != null
                   ? customizeAuthorizationRequest(authorizationRequest)
                   : null;
    }

    private OAuth2AuthorizationRequest customizeAuthorizationRequest(
        OAuth2AuthorizationRequest authorizationRequest) {
        Map<String, Object> attributes =
            new HashMap<>(authorizationRequest.getAttributes());
        Map<String, Object> additionalParameters =
            new HashMap<>(authorizationRequest.getAdditionalParameters());

        addAuthzDetailsParameters(attributes, additionalParameters);
        addPkceParameters(attributes, additionalParameters);

        return OAuth2AuthorizationRequest.from(authorizationRequest)
                                         .attributes(attributes)
                                         .additionalParameters(additionalParameters)
                                         .build();
    }

    @SneakyThrows
    private void addAuthzDetailsParameters(Map<String, Object> attributes,
                                           Map<String, Object> additionalParameters) {
        attributes.put(
            OAuth2Constants.OAUTH2_AUTHORIZATION_DETAILS_PARAMETER,
            ansattportenAuthzConfig.authorizationDetails());

        String authDetailsListString =
            new ObjectMapper().writeValueAsString(
                List.of(ansattportenAuthzConfig.authorizationDetails()));
        additionalParameters.put(
            OAuth2Constants.OAUTH2_AUTHORIZATION_DETAILS_PARAMETER, authDetailsListString);
    }

    private void addPkceParameters(Map<String, Object> attributes,
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
