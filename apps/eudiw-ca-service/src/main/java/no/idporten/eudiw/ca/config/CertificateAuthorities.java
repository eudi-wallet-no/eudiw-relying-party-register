package no.idporten.eudiw.ca.config;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Map;
import java.util.Optional;


/**
 * Config of certificate authorities:  One root and a list of intermediates.
 */
@Validated
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "eudiw-ca")
public class CertificateAuthorities implements InitializingBean {

    @Getter
    @NotNull
    private final CertificateAuthority root;
    @NotNull
    private final Map<String, CertificateAuthority> intermediates;

    public List<CertificateAuthority> getIntermediates() {
        return List.copyOf(intermediates.values());
    }

    public CertificateAuthority findIntermediate(String name) {
        return Optional.ofNullable(intermediates.get(name)).orElseThrow(() -> new RuntimeException("Unknown intermediate CA"));
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        root.init();
        root.validate(root);
        for (CertificateAuthority intermediate : intermediates.values()) {
            intermediate.init();
            intermediate.validate(root);
        }
    }

}
