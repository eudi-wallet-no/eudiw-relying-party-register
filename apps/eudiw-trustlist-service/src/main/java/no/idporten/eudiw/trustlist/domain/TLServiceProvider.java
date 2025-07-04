package no.idporten.eudiw.trustlist.domain;

import java.util.List;

public record TLServiceProvider(TSName name, TSName tradeName, TSUri informationUri, List<TLRpAccessService> rpAccessServices) {
    public TLServiceProvider {
        if (name == null) {
            throw new IllegalArgumentException("TLServiceProvider: Name must not be null");
        }
        if (tradeName == null) {
            throw new IllegalArgumentException("TLServiceProvider: TradeName must not be null");
        }
        if (informationUri == null) {
            throw new IllegalArgumentException("TLServiceProvider: Information-URI must not be null");
        }
        if (rpAccessServices == null || rpAccessServices.isEmpty()) {
            throw new IllegalArgumentException("TLServiceProvider: RP Access Services must not be null or empty");
        }
    }
}
