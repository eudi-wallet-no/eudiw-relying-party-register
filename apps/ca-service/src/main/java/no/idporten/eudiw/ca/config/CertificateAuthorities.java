package no.idporten.eudiw.ca.config;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.ca.exception.CertificateAuthorityException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;

import java.util.Map;
import java.util.Optional;


/**
 * Config of certificate authorities:  One root and a list of intermediates.
 */
@Validated
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "eudiw-ca")
public class CertificateAuthorities implements InitializingBean {

    @NotNull
    private final Map<String, CertificateAuthority> roots;

    @NotNull
    private final Map<String, CertificateAuthority> intermediates;

    public CertificateAuthority findRoot(String name) {
        return Optional.ofNullable(roots.get(name)).orElseThrow(() -> new CertificateAuthorityException("invalid_request", "Unknown root CA", HttpStatus.NOT_FOUND));
    }

    public CertificateAuthority findIntermediate(String name) {
        return Optional.ofNullable(intermediates.get(name)).orElseThrow(() -> new CertificateAuthorityException("invalid_request", "Unknown intermediate CA", HttpStatus.NOT_FOUND));
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        for (Map.Entry<String, CertificateAuthority> rootEntry : roots.entrySet()) {
            rootEntry.getValue().init(rootEntry.getKey());
            rootEntry.getValue().validate(rootEntry.getValue());
        }
        for (Map.Entry<String, CertificateAuthority> intermediateEntry : intermediates.entrySet()) {
            intermediateEntry.getValue().init(intermediateEntry.getKey());
            intermediateEntry.getValue().validate(findRoot(intermediateEntry.getValue().getRoot()));
        }
    }

}
