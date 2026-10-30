package no.eudiw.rp.register.api.v2;

import no.eudiw.rp.register.exception.BadRequestException;
import no.eudiw.rp.register.repository.RelyingPartyInstanceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class V2ApiServiceTest {

    private final RelyingPartyInstanceRepository relyingPartyInstanceRepository =
        mock(RelyingPartyInstanceRepository.class);
    private final V2ApiService service = new V2ApiService(
        null, null, relyingPartyInstanceRepository, null, null, null, null, null, null, null, null);

    @ParameterizedTest
    @CsvSource({
        "orgno, walletRelyingPartyService.walletRelyingParty.orgno",
        "name, walletRelyingPartyService.serviceTradeName",
        "serviceTradeName, walletRelyingPartyService.serviceTradeName"
    })
    @DisplayName("Sort instance search by the organization number or service name that admin selects")
    void shouldSortInstanceSearchByAdminSortKey(String sortKey, String expectedProperty) {
        when(relyingPartyInstanceRepository.searchV2(
            any(), any(), anyString(), anyBoolean(), anySet(), anyInt(), any(Pageable.class)
        )).thenReturn(Page.empty());

        service.searchRelyingPartyInstances(null, null, null, false, null, 0, 25, sortKey);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(relyingPartyInstanceRepository).searchV2(
            any(), any(), anyString(), anyBoolean(), anySet(), anyInt(), pageable.capture());
        assertEquals(Sort.by(expectedProperty, "id"), pageable.getValue().getSort());
    }

    @Test
    @DisplayName("Reject internal entity paths as instance sort keys")
    void shouldRejectInternalEntityPathAsSortKey() {
        assertThrows(BadRequestException.class, () -> service.searchRelyingPartyInstances(
            null, null, null, false, null, 0, 25, "walletRelyingPartyService.serviceTradeName"));
    }
}
