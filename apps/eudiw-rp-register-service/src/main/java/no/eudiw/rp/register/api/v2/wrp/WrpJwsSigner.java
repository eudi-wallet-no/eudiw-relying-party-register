package no.eudiw.rp.register.api.v2.wrp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import no.eudiw.rp.register.config.WrpApiProperties;
import no.idporten.lib.keystore.KeystoreManager;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;

@Service
public class WrpJwsSigner {

    private final KeystoreManager keystoreManager;
    private final WrpApiProperties properties;
    private final ObjectMapper objectMapper;

    public WrpJwsSigner(
        KeystoreManager keystoreManager,
        WrpApiProperties properties,
        ObjectMapper objectMapper
    ) {
        this.keystoreManager = keystoreManager;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public String sign(WalletRelyingPartyResource data) {
        return signPayload(data, null);
    }

    public String signCollection(
        List<WalletRelyingPartyResource> data,
        String nextCursor,
        boolean hasNextPage
    ) {
        return signPayload(data, new WrpPagination(nextCursor, hasNextPage));
    }

    private String signPayload(Object data, WrpPagination pagination) {
        String payload = serialize(new SignedWrpPayload(
            properties.issuer(),
            Instant.now().getEpochSecond(),
            data,
            pagination
        ));
        JWSObject jws = new JWSObject(
            new JWSHeader.Builder(JWSAlgorithm.ES256)
                .type(JOSEObjectType.JWT)
                .keyID(properties.signingKeyId())
                .build(),
            new Payload(payload)
        );
        try {
            jws.sign(new ECDSASigner(
                keystoreManager.getKeyProvider(properties.signingKeystore()).privateKey(),
                Curve.P_256
            ));
        } catch (JOSEException e) {
            throw new IllegalStateException("Unable to sign WRP response", e);
        }
        return jws.serialize();
    }

    private String serialize(SignedWrpPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JacksonException e) {
            throw new IllegalStateException("Unable to serialize WRP response for signing", e);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record SignedWrpPayload(String iss, long iat, Object data, WrpPagination pagination) { }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record WrpPagination(
        @JsonProperty("next_cursor") String nextCursor,
        @JsonProperty("has_next_page") boolean hasNextPage
    ) { }
}
