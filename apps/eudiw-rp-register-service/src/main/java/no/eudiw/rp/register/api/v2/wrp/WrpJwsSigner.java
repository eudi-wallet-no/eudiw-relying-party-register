package no.eudiw.rp.register.api.v2.wrp;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import no.idporten.lib.keystore.KeystoreManager;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;

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
        String payload = serialize(new SignedWrpPayload(
            properties.issuer(),
            Instant.now().getEpochSecond(),
            data
        ));
        JWSObject jws = new JWSObject(
            new JWSHeader.Builder(JWSAlgorithm.ES256).type(JOSEObjectType.JWT).build(),
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

    private record SignedWrpPayload(String iss, long iat, WalletRelyingPartyResource data) { }
}
