package no.eudiw.rp.register.api.v1;

import no.eudiw.rp.register.api.v1.resource.relyingparty.SearchRelyingPartyResource;
import no.eudiw.rp.register.api.v1.resource.relyingparty.CreateRelyingPartyResource;
import no.eudiw.rp.register.api.v1.resource.relyingparty.RelyingPartyEntitlementResource;
import no.eudiw.rp.register.domain.ServiceCredentialIssuerUrl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
            mock(V1ContractMapper.class)
        );

        service.searchRelyingParties(new SearchRelyingPartyResource("term").withSortKey(sortKey));

        ArgumentCaptor<PageRequest> pageRequest = ArgumentCaptor.forClass(PageRequest.class);
        verify(relyingPartyService).searchRelyingParties(
            anyString(), anyList(), anyBoolean(), anyBoolean(), pageRequest.capture()
        );
        assertEquals(expectedSort, pageRequest.getValue().getSort());
    }

    @Test
    @DisplayName("Should keep different URLs for role URIs with different letter case or space characters")
    void shouldKeepDifferentUrlsForRoleUrisWithDifferentLetterCaseOrSpaceCharacters() {
        RelyingPartyService relyingPartyService = mock(RelyingPartyService.class);
        V1ContractMapper mapper = mock(V1ContractMapper.class);
        V1ApiService service = new V1ApiService(
            relyingPartyService,
            mock(RelyingPartyCertificateService.class),
            mock(EntitlementService.class),
            mock(CredentialIssuersService.class),
            mapper
        );
        List<String> roles = List.of("https://roles.example/issuer", "https://roles.example/Issuer",
            "https://roles.example/issuer ");
        List<RelyingPartyEntitlementResource> entitlements = List.of(
            new RelyingPartyEntitlementResource(roles.get(0), "Issuer", "https://issuer.example/first"),
            new RelyingPartyEntitlementResource(roles.get(1), "Issuer", "https://issuer.example/second"),
            new RelyingPartyEntitlementResource(roles.get(2), "Issuer", "https://issuer.example/third")
        );
        when(mapper.toDomainRelyingPartyEntitlements(entitlements)).thenReturn(List.of());
        when(mapper.toDomainRelyingPartyEaas(List.of())).thenReturn(List.of());

        service.createRelyingParty(new CreateRelyingPartyResource("123456789", "Service", entitlements, List.of()));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ServiceCredentialIssuerUrl>> urls = ArgumentCaptor.forClass(List.class);
        verify(relyingPartyService).createRelyingParty(
            eq("123456789"), eq("Service"), eq(List.of()), eq(List.of()), urls.capture());
        assertEquals(roles, urls.getValue().stream().map(ServiceCredentialIssuerUrl::entitlement).toList());
        assertEquals(entitlements.stream().map(RelyingPartyEntitlementResource::credentialIssuerUrl).toList(),
            urls.getValue().stream().map(ServiceCredentialIssuerUrl::credentialIssuerUrl).toList());
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
