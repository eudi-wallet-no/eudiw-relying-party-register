package no.eudiw.rp.register.api.v2.wrp;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import no.idporten.lib.keystore.KeystoreManager;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;

@RestController
@RequestMapping("/wrp")
@RequiredArgsConstructor
@Tag(name = "wrp-api", description = "Public Wallet Relying Party API")
public class WrpJwksEndpoint {
    private static final MediaType JWKS_MEDIA_TYPE = MediaType.parseMediaType("application/jwk-set+json");

    private final KeystoreManager keystoreManager;
    private final WrpApiProperties properties;

    @Operation(
        summary = "Get the WRP signing public key set",
        responses = @ApiResponse(
            responseCode = "200",
            description = "Public keys used to verify signed WRP responses",
            content = @Content(
                mediaType = "application/jwk-set+json",
                schema = @Schema(type = "object")
            )
        )
    )
    @GetMapping(value = "/jwks", produces = "application/jwk-set+json")
    public ResponseEntity<String> getJwks() {
        return ResponseEntity.ok()
            .contentType(JWKS_MEDIA_TYPE)
            .body(new JWKSet(createSigningJwk()).toString());
    }

    private ECKey createSigningJwk() {
        PublicKey publicKey = keystoreManager.getKeyProvider(properties.signingKeystore()).publicKey();
        if (!(publicKey instanceof ECPublicKey ecPublicKey)
            || !Curve.P_256.equals(Curve.forECParameterSpec(ecPublicKey.getParams()))) {
            throw new IllegalStateException("WRP signing public key must use the P-256 curve");
        }

        return new ECKey.Builder(Curve.P_256, ecPublicKey)
            .keyID(properties.signingKeyId())
            .algorithm(JWSAlgorithm.ES256)
            .keyUse(KeyUse.SIGNATURE)
            .build();
    }
}
