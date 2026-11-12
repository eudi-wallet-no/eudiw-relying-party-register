package no.eudiw.rp.register.repository;

import no.eudiw.rp.register.domain.WalletRelyingParty;

import java.util.List;
import java.util.UUID;

public interface WalletRelyingPartyCollectionQuery {
    List<WalletRelyingParty> findPage(
        WrpSearchCriteria criteria,
        Long cursorCreatedMs,
        UUID cursorId,
        int pageSize
    );
}
