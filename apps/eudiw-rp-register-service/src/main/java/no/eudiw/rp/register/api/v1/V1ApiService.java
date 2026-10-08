package no.eudiw.rp.register.api.v1;

import lombok.RequiredArgsConstructor;
import no.eudiw.rp.register.api.v1.resource.CredentialIssuerUrlsResource;
import no.eudiw.rp.register.api.v1.resource.certificates.IssuerCsrResource;
import no.eudiw.rp.register.api.v1.resource.certificates.RelyingPartyCertificateResource;
import no.eudiw.rp.register.api.v1.resource.certificates.RelyingPartyCertificatesResource;
import no.eudiw.rp.register.api.v1.resource.certificates.RelyingPartyCsrResource;
import no.eudiw.rp.register.api.v1.resource.entitlements.EntitlementsResource;
import no.eudiw.rp.register.api.v1.resource.relyingparty.CreateRelyingPartyResource;
import no.eudiw.rp.register.api.v1.resource.relyingparty.EditRelyingPartyResource;
import no.eudiw.rp.register.api.v1.resource.relyingparty.RelyingPartyResource;
import no.eudiw.rp.register.api.v1.resource.relyingparty.SearchRelyingPartyResource;
import no.eudiw.rp.register.domain.Entitlement;
import no.eudiw.rp.register.domain.certificates.AccessCertificate;
import no.eudiw.rp.register.domain.certificates.IssuerCertificate;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyEaa;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyEntitlement;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyInstance;
import no.eudiw.rp.register.service.CredentialIssuersService;
import no.eudiw.rp.register.service.EntitlementService;
import no.eudiw.rp.register.service.RelyingPartyService;
import no.eudiw.rp.register.service.RelyingPartyCertificateService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class V1ApiService {

    private final RelyingPartyService relyingPartyInstanceService;
    private final RelyingPartyCertificateService certificateService;
    private final EntitlementService entitlementService;
    private final CredentialIssuersService credentialIssuersService;
    private final V1ContractMapper v1ContractMapper;

    @Transactional
    public RelyingPartyResource createRelyingParty(CreateRelyingPartyResource request) {
        List<RelyingPartyEntitlement> domainEntitlements =
            v1ContractMapper.toDomainRelyingPartyEntitlements(request.relyingPartyEntitlements());
        List<RelyingPartyEaa> domainEaas = v1ContractMapper.toDomainRelyingPartyEaas(request.relyingPartyEaas());
        RelyingPartyInstance createdInstance = relyingPartyInstanceService.createRelyingParty(
            request.orgNr(), request.tradeName(), domainEntitlements, domainEaas);
        return v1ContractMapper.toV1RelyingPartyResource(createdInstance);
    }

    @Transactional
    public RelyingPartyResource updateRelyingParty(UUID id, EditRelyingPartyResource request) {
        RelyingPartyInstance updatedInstance = relyingPartyInstanceService.updateRelyingParty(
            id,
            request.tradeName(),
            request.active(),
            v1ContractMapper.toDomainRelyingPartyEntitlements(request.relyingPartyEntitlements()),
            v1ContractMapper.toDomainRelyingPartyEaas(request.relyingPartyEaas())
        );
        return v1ContractMapper.toV1RelyingPartyResource(updatedInstance);
    }

    @Transactional
    public void deleteRelyingParty(UUID id) {
        relyingPartyInstanceService.deleteRelyingParty(id);
    }

    @Transactional(readOnly = true)
    public RelyingPartyResource findRelyingParty(UUID id) {
        RelyingPartyInstance instance = relyingPartyInstanceService.findRelyingParty(id);
        return v1ContractMapper.toV1RelyingPartyResource(instance);
    }

    @Transactional(readOnly = true)
    public PagedModel<RelyingPartyResource> searchRelyingParties(SearchRelyingPartyResource request) {
        PageRequest pageRequest = PageRequest.of(
            request.getPage(),
            request.getPageSize(),
            sortFor(request.getSortKey())
        );
        return new PagedModel<>(relyingPartyInstanceService.searchRelyingParties(
            request.getSearchTerm(),
            request.getRequiredEntitlements(),
            request.isIncludeInactive(),
            request.isHideSyntheticOrgnos(),
            pageRequest
        ).map(v1ContractMapper::toV1RelyingPartyResource));
    }

    private static Sort sortFor(String sortKey) {
        return switch (sortKey) {
            case "name", "tradeName" -> Sort.by("walletRelyingPartyService.serviceTradeName");
            case "orgno", "legalEntity.orgno" -> Sort.by("walletRelyingPartyService.walletRelyingParty.orgno");
            case "createdMs" -> Sort.by("createdMs");
            case "lastUpdatedMs" -> Sort.by("lastUpdatedMs");
            case null, default -> Sort.unsorted();
        };
    }

    @Transactional(readOnly = true)
    public EntitlementsResource findAllEntitlements(boolean includeInactive) {
        List<Entitlement> entitlements = entitlementService.findAllEntitlements(includeInactive);
        return v1ContractMapper.toV1EntitlementsResource(entitlements);
    }

    @Transactional(readOnly = true)
    public CredentialIssuerUrlsResource getRegisteredCredentialIssuerUrls() {
        return new CredentialIssuerUrlsResource(credentialIssuersService.getRegisteredCredentialIssuerUrls());
    }

    @Transactional(readOnly = true)
    public RelyingPartyCertificatesResource getCertificatesForRelyingParty(UUID relyingPartyId) {
        List<RelyingPartyCertificateResource> certificates =
            certificateService.getCertificatesForRelyingParty(relyingPartyId).stream()
                .map(v1ContractMapper::toV1CertificateResource)
                .toList();
        return new RelyingPartyCertificatesResource(certificates);
    }

    @Transactional(readOnly = true)
    public RelyingPartyCertificateResource getCertificate(UUID certificateId, UUID relyingPartyId) {
        AccessCertificate certificate = certificateService.getCertificate(certificateId, relyingPartyId);
        return v1ContractMapper.toV1CertificateResource(certificate);
    }

    @Transactional(readOnly = true)
    public RelyingPartyCertificateResource getIssuerCertificate(UUID certificateId, UUID relyingPartyId) {
        IssuerCertificate certificate = certificateService.getIssuerCertificate(certificateId, relyingPartyId);
        return v1ContractMapper.toV1CertificateResource(certificate);
    }

    @Transactional(readOnly = true)
    public RelyingPartyCertificatesResource getAllIssuerCertificatesFromRelyingParty(UUID relyingPartyId) {
        List<RelyingPartyCertificateResource> certificates =
            certificateService.getAllIssuerCertificatesFromRelyingParty(relyingPartyId).stream()
                .map(v1ContractMapper::toV1CertificateResource)
                .toList();
        return new RelyingPartyCertificatesResource(certificates);
    }

    @Transactional
    public RelyingPartyCertificateResource requestIssuerCertificate(
        UUID relyingPartyId,
        IssuerCsrResource request
    ) {
        IssuerCertificate certificate = certificateService.requestIssuerCertificate(
            relyingPartyId, request.csr(), request.entitlement());
        return v1ContractMapper.toV1CertificateResource(certificate);
    }

    @Transactional
    public RelyingPartyCertificateResource requestAccessCertificateForRelyingParty(
        UUID relyingPartyId,
        RelyingPartyCsrResource request
    ) {
        AccessCertificate certificate =
            certificateService.requestAccessCertificateForRelyingParty(relyingPartyId, request.csr());
        return v1ContractMapper.toV1CertificateResource(certificate);
    }

    @Transactional
    public void revokeAccessCertificate(UUID certificateId, UUID relyingPartyId) {
        certificateService.revokeAccessCertificate(certificateId, relyingPartyId);
    }

    @Transactional
    public void revokeIssuerCertificate(UUID certificateId, UUID relyingPartyId) {
        certificateService.revokeIssuerCertificate(certificateId, relyingPartyId);
    }
}
