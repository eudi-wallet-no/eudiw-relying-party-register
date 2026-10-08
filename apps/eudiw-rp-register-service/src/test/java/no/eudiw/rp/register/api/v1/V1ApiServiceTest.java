package no.eudiw.rp.register.api.v1;

import no.eudiw.rp.register.api.v1.resource.relyingparty.SearchRelyingPartyResource;
import no.eudiw.rp.register.service.CredentialIssuersService;
import no.eudiw.rp.register.service.EntitlementService;
import no.eudiw.rp.register.service.RelyingPartyService;
import no.eudiw.rp.register.service.RelyingPartyCertificateService;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class V1ApiServiceTest {

    @ParameterizedTest
    @MethodSource("sortKeys")
    void searchMapsV1SortKeysToEntityProperties(String sortKey, Sort expectedSort) {
        RelyingPartyService relyingPartyService = mock(RelyingPartyService.class);
        when(relyingPartyService.searchRelyingParties(
            anyString(), anyList(), anyBoolean(), anyBoolean(), any(PageRequest.class)
        )).thenReturn(new PageImpl<>(List.of()));

        V1ApiService service = new V1ApiService(
            relyingPartyService,
            mock(RelyingPartyCertificateService.class),
            mock(EntitlementService.class),
            mock(CredentialIssuersService.class),
            mock(V1DataConverter.class)
        );

        service.searchRelyingParties(new SearchRelyingPartyResource("term").withSortKey(sortKey));

        ArgumentCaptor<PageRequest> pageRequest = ArgumentCaptor.forClass(PageRequest.class);
        verify(relyingPartyService).searchRelyingParties(
            anyString(), anyList(), anyBoolean(), anyBoolean(), pageRequest.capture()
        );
        assertEquals(expectedSort, pageRequest.getValue().getSort());
    }

    private static Stream<Arguments> sortKeys() {
        return Stream.of(
            arguments("name", Sort.by("walletRelyingPartyService.serviceTradeName")),
            arguments("tradeName", Sort.by("walletRelyingPartyService.serviceTradeName")),
            arguments("orgno", Sort.by("walletRelyingPartyService.walletRelyingParty.orgno")),
            arguments("legalEntity.orgno", Sort.by("walletRelyingPartyService.walletRelyingParty.orgno")),
            arguments("createdMs", Sort.by("createdMs")),
            arguments("lastUpdatedMs", Sort.by("lastUpdatedMs")),
            arguments("unsorted", Sort.unsorted()),
            arguments("unknown", Sort.unsorted()),
            arguments("", Sort.unsorted()),
            arguments(null, Sort.unsorted())
        );
    }
}
