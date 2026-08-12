package no.idporten.eudiw.trustlist.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.idporten.eudiw.trustlist.domain.etsi602.Trustlist;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.Map;

@Validated
@ConfigurationProperties(prefix = "trustlist-service")
public record Trustlist602Properties(@Valid @NotNull Map<String, Trustlist> tsl602

) {

    public final static String TSL_ACA = "tsl-aca";
    public final static String TSL_PID = "tsl-pid";
    public final static String TSL_WALLET = "tsl-wallet";

    private Trustlist getTrustlist(String key) {
        Trustlist trustlist = tsl602.get(key);
        if (trustlist == null) {
            throw new IllegalStateException("Missing required trustlist configuration for key '" + key + "' in 'trustlist-service.tsl602'");
        }
        return trustlist;
    }

    public Trustlist getAcaTrustlist(){
        return getTrustlist(TSL_ACA);
    }

    public Trustlist getPidTrustlist(){
        return getTrustlist(TSL_PID);
    }
    public Trustlist getWalletTrustlist(){
        return getTrustlist(TSL_WALLET);
    }
}
