package no.eudiw.rp.register.api.v2;

import lombok.RequiredArgsConstructor;
import no.eudiw.rp.register.api.v2.resource.V2Resources;
import no.eudiw.rp.register.domain.Entitlement;
import no.eudiw.rp.register.domain.IntendedUse;
import no.eudiw.rp.register.domain.WalletRelyingParty;
import no.eudiw.rp.register.domain.WalletRelyingPartyService;
import no.eudiw.rp.register.domain.certificates.AccessCertificate;
import no.eudiw.rp.register.domain.certificates.IssuerCertificate;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyEntitlement;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyInstance;
import no.eudiw.rp.register.exception.BadRequestException;
import no.eudiw.rp.register.exception.NotFoundException;
import no.eudiw.rp.register.repository.RelyingPartyInstanceRepository;
import no.eudiw.rp.register.repository.WalletRelyingPartyRepository;
import no.eudiw.rp.register.repository.WalletRelyingPartyServiceRepository;
import no.eudiw.rp.register.service.EntitlementService;
import no.eudiw.rp.register.service.RelyingPartyCertificateService;
import no.eudiw.rp.register.service.RelyingPartyService;
import no.eudiw.rp.register.service.WalletRelyingPartyLookupService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(isolation = Isolation.READ_COMMITTED)
public class V2ApiService {

    private static final int MAX_PAGE_SIZE = 100;

    private final WalletRelyingPartyRepository walletRelyingPartyRepository;
    private final WalletRelyingPartyServiceRepository walletRelyingPartyServiceRepository;
    private final RelyingPartyInstanceRepository relyingPartyInstanceRepository;
    private final WalletRelyingPartyLookupService walletRelyingPartyLookupService;
    private final V2ContractMapper v2ContractMapper;
    private final EntitlementService entitlementService;
    private final RelyingPartyCertificateService certificateService;
    private final RelyingPartyService relyingPartyService;

    public V2Resources.WalletRelyingPartyResource createWalletRelyingParty(
        V2Resources.CreateWalletRelyingPartyResource request
    ) {
        WalletRelyingParty walletRelyingParty =
            walletRelyingPartyLookupService.createWalletRelyingParty(request.orgno());

        return v2ContractMapper.toWalletRelyingPartyResponse(walletRelyingParty);
    }

    public V2Resources.WalletRelyingPartyResource getWalletRelyingParty(UUID id) {
        WalletRelyingParty walletRelyingParty = findWalletRelyingParty(id);
        return v2ContractMapper.toWalletRelyingPartyResponse(walletRelyingParty);
    }

    public Page<V2Resources.WalletRelyingPartyResource> searchWalletRelyingParties(
        String orgno,
        int page,
        int pageSize,
        String sort
    ) {
        String defaultSort = "orgno";
        Set<String> allowedSortProperties = Set.of("orgno", "tradeName", "createdMs", "lastUpdatedMs");

        PageRequest pageRequest = pageRequest(
            page, pageSize, sort, defaultSort, allowedSortProperties);
        Page<WalletRelyingParty> walletRelyingParties = orgno == null
            ? walletRelyingPartyRepository.findAll(pageRequest)
            : walletRelyingPartyRepository.findAllByOrgno(orgno, pageRequest);

        return walletRelyingParties.map(v2ContractMapper::toWalletRelyingPartyResponse);
    }

    public V2Resources.WalletRelyingPartyResource updateWalletRelyingParty(
        UUID id,
        V2Resources.WalletRelyingPartyWriteResource request
    ) {
        WalletRelyingParty walletRelyingParty = walletRelyingPartyRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new NotFoundException("WalletRelyingParty not found"));

        v2ContractMapper.updateWalletRelyingParty(walletRelyingParty, request);

        return v2ContractMapper.toWalletRelyingPartyResponse(
            walletRelyingPartyRepository.saveAndFlush(walletRelyingParty));
    }

    public V2Resources.WalletRelyingPartyServiceResource createWalletRelyingPartyService(
        UUID walletRelyingPartyId,
        V2Resources.WalletRelyingPartyServiceWriteResource request
    ) {
        WalletRelyingParty walletRelyingParty = walletRelyingPartyRepository.findByIdForUpdate(walletRelyingPartyId)
            .orElseThrow(() -> new NotFoundException("WalletRelyingParty not found"));

        if (!walletRelyingParty.isActive()) {
            throw new BadRequestException(
                "Cannot create a WalletRelyingPartyService for an inactive WalletRelyingParty");
        }

        WalletRelyingPartyService walletRelyingPartyService = v2ContractMapper.toWalletRelyingPartyService(
            request, walletRelyingParty, List.of());
        walletRelyingParty.getServices().add(walletRelyingPartyService);

        return v2ContractMapper.toWalletRelyingPartyServiceResponse(
            walletRelyingPartyServiceRepository.saveAndFlush(walletRelyingPartyService));
    }

    public V2Resources.WalletRelyingPartyServiceResource getWalletRelyingPartyService(UUID id) {
        return v2ContractMapper.toWalletRelyingPartyServiceResponse(findWalletRelyingPartyService(id));
    }

    public Page<V2Resources.WalletRelyingPartyServiceResource> searchWalletRelyingPartyServices(
        UUID partyId,
        int page,
        int pageSize,
        String sort
    ) {
        if (partyId == null) {
            throw new BadRequestException("partyId is required");
        }

        Page<WalletRelyingPartyService> walletRelyingPartyServices =
            walletRelyingPartyServiceRepository.findAllByWalletRelyingParty_Id(
            partyId, pageRequest(
                page, pageSize, sort, "serviceTradeName",
                Set.of("serviceTradeName", "createdMs", "lastUpdatedMs")));

        return walletRelyingPartyServices.map(v2ContractMapper::toWalletRelyingPartyServiceResponse);
    }

    public V2Resources.WalletRelyingPartyServiceResource updateWalletRelyingPartyService(
        UUID id,
        V2Resources.WalletRelyingPartyServiceWriteResource request
    ) {
        walletRelyingPartyRepository.findByServiceIdForUpdate(id)
            .orElseThrow(() -> new NotFoundException("WalletRelyingPartyService not found"));

        WalletRelyingPartyService walletRelyingPartyService = findWalletRelyingPartyService(id);
        v2ContractMapper.updateWalletRelyingPartyService(walletRelyingPartyService, request);

        return v2ContractMapper.toWalletRelyingPartyServiceResponse(
            walletRelyingPartyServiceRepository.saveAndFlush(walletRelyingPartyService));
    }

    public V2Resources.RelyingPartyInstanceResource createRelyingPartyInstance(
        UUID walletRelyingPartyServiceId,
        V2Resources.RelyingPartyInstanceWriteResource request
    ) {
        WalletRelyingParty walletRelyingParty = walletRelyingPartyRepository
            .findByServiceIdForUpdate(walletRelyingPartyServiceId)
            .orElseThrow(() -> new NotFoundException("WalletRelyingPartyService not found"));

        if (!walletRelyingParty.isActive()) {
            throw new BadRequestException("Cannot create a RelyingPartyInstance for an inactive WalletRelyingParty");
        }

        WalletRelyingPartyService walletRelyingPartyService =
            findWalletRelyingPartyService(walletRelyingPartyServiceId);
        if (!walletRelyingPartyService.isActive()) {
            throw new BadRequestException(
                "Cannot create a RelyingPartyInstance for an inactive WalletRelyingPartyService");
        }

        var relyingPartyEntitlements = v2ContractMapper.toDomainRelyingPartyEntitlements(
            request.relyingPartyEntitlements());
        relyingPartyService.validateEntitlements(relyingPartyEntitlements);

        RelyingPartyInstance relyingPartyInstance = new RelyingPartyInstance(
            relyingPartyEntitlements,
            v2ContractMapper.toDomainRelyingPartyEaas(request.relyingPartyEaas()),
            List.of()
        );

        relyingPartyInstance.setWalletRelyingPartyService(walletRelyingPartyService);
        walletRelyingPartyService.getRelyingPartyInstances().add(relyingPartyInstance);

        return v2ContractMapper.toRelyingPartyInstanceResponse(
            relyingPartyInstanceRepository.saveAndFlush(relyingPartyInstance));
    }

    public V2Resources.RelyingPartyInstanceResource getRelyingPartyInstance(UUID id) {
        return v2ContractMapper.toRelyingPartyInstanceResponse(findRelyingPartyInstance(id));
    }

    public Page<V2Resources.RelyingPartyInstanceResource> searchRelyingPartyInstances(
        UUID serviceId,
        String orgno,
        String searchTerm,
        boolean includeInactive,
        List<String> requiredEntitlements,
        int page,
        int pageSize,
        String sort
    ) {
        Set<String> uniqueRequiredEntitlements =
            new HashSet<>(Objects.requireNonNullElse(requiredEntitlements, List.of()));
        Page<RelyingPartyInstance> relyingPartyInstances = relyingPartyInstanceRepository.searchV2(
            serviceId,
            orgno,
            searchTerm == null ? "" : searchTerm,
            includeInactive,
            uniqueRequiredEntitlements,
            uniqueRequiredEntitlements.size(),
            pageRequest(page, pageSize, sort, "createdMs", Set.of("createdMs", "lastUpdatedMs"))
        );

        return relyingPartyInstances.map(v2ContractMapper::toRelyingPartyInstanceResponse);
    }

    public V2Resources.RelyingPartyInstanceResource updateRelyingPartyInstance(
        UUID id,
        V2Resources.RelyingPartyInstanceWriteResource request
    ) {
        walletRelyingPartyRepository.findByInstanceIdForUpdate(id)
            .orElseThrow(() -> new NotFoundException("RelyingPartyInstance not found"));

        RelyingPartyInstance relyingPartyInstance = findRelyingPartyInstance(id);
        List<RelyingPartyEntitlement> requestedEntitlements =
            v2ContractMapper.toDomainRelyingPartyEntitlements(request);
        relyingPartyService.validateEntitlementsForUpdate(
            relyingPartyInstance.getRelyingPartyEntitlements(), requestedEntitlements);
        v2ContractMapper.updateRelyingPartyInstance(relyingPartyInstance, request, requestedEntitlements);

        return v2ContractMapper.toRelyingPartyInstanceResponse(
            relyingPartyInstanceRepository.saveAndFlush(relyingPartyInstance));
    }

    public List<V2Resources.IntendedUseResource> getIntendedUses(UUID serviceId) {
        WalletRelyingPartyService walletRelyingPartyService = findWalletRelyingPartyService(serviceId);
        return walletRelyingPartyService.getIntendedUses().stream()
            .map(v2ContractMapper::toIntendedUseResponse)
            .toList();
    }

    public V2Resources.IntendedUseResource createIntendedUse(
        UUID serviceId,
        V2Resources.CreateIntendedUseResource request
    ) {
        WalletRelyingPartyService walletRelyingPartyService = findActiveWalletRelyingPartyServiceForUpdate(serviceId);
        IntendedUse intendedUse = v2ContractMapper.toDomainIntendedUse(request);
        intendedUse.setWalletRelyingPartyService(walletRelyingPartyService);
        walletRelyingPartyService.getIntendedUses().add(intendedUse);
        walletRelyingPartyServiceRepository.flush();

        return v2ContractMapper.toIntendedUseResponse(intendedUse);
    }

    public V2Resources.IntendedUseReplacementResource replaceIntendedUse(
        UUID serviceId,
        UUID previousIntendedUseId,
        V2Resources.CreateIntendedUseResource request
    ) {
        WalletRelyingPartyService walletRelyingPartyService = findActiveWalletRelyingPartyServiceForUpdate(serviceId);
        IntendedUse previousIntendedUse = walletRelyingPartyService.getIntendedUses().stream()
            .filter(candidate -> previousIntendedUseId.equals(candidate.getId()))
            .findFirst()
            .orElseThrow(() -> new NotFoundException("IntendedUse not found"));

        if (previousIntendedUse.getRevokedAt() != null) {
            throw new BadRequestException("IntendedUse is already revoked");
        }

        String now = LocalDate.now(ZoneOffset.UTC).toString();
        IntendedUse currentIntendedUse = v2ContractMapper.toDomainIntendedUse(request, now);
        previousIntendedUse.setRevokedAt(now);
        currentIntendedUse.setWalletRelyingPartyService(walletRelyingPartyService);
        walletRelyingPartyService.getIntendedUses().add(currentIntendedUse);
        walletRelyingPartyServiceRepository.flush();

        return new V2Resources.IntendedUseReplacementResource(
            v2ContractMapper.toIntendedUseResponse(previousIntendedUse),
            v2ContractMapper.toIntendedUseResponse(currentIntendedUse)
        );
    }

    public V2Resources.IntendedUseResource revokeIntendedUse(UUID serviceId, UUID intendedUseId) {
        WalletRelyingPartyService walletRelyingPartyService = findActiveWalletRelyingPartyServiceForUpdate(serviceId);
        IntendedUse intendedUse = walletRelyingPartyService.getIntendedUses().stream()
            .filter(candidate -> intendedUseId.equals(candidate.getId()))
            .findFirst()
            .orElseThrow(() -> new NotFoundException("IntendedUse not found"));

        if (intendedUse.getRevokedAt() != null) {
            throw new BadRequestException("IntendedUse is already revoked");
        }

        intendedUse.setRevokedAt(LocalDate.now(ZoneOffset.UTC).toString());
        walletRelyingPartyServiceRepository.flush();

        return v2ContractMapper.toIntendedUseResponse(intendedUse);
    }

    @Transactional(readOnly = true)
    public V2Resources.EntitlementsResource getEntitlements(boolean includeInactive) {
        List<V2Resources.EntitlementResource> entitlements =
            entitlementService.findAllEntitlements(includeInactive).stream()
                .map(this::toEntitlementResponse)
                .toList();
        return new V2Resources.EntitlementsResource(entitlements);
    }

    @Transactional(readOnly = true)
    public V2Resources.CertificatesResource getAccessCertificates(UUID instanceId) {
        return new V2Resources.CertificatesResource(certificateService.getCertificatesForRelyingParty(instanceId)
            .stream().map(v2ContractMapper::toCertificateResponse).toList());
    }

    @Transactional(readOnly = true)
    public V2Resources.CertificatesResource getIssuerCertificates(UUID instanceId) {
        return new V2Resources.CertificatesResource(certificateService.getAllIssuerCertificatesFromRelyingParty(instanceId)
            .stream().map(v2ContractMapper::toCertificateResponse).toList());
    }

    @Transactional(readOnly = true)
    public V2Resources.CertificateResource getAccessCertificate(UUID instanceId, UUID certificateId) {
        AccessCertificate certificate = certificateService.getCertificate(certificateId, instanceId);
        return v2ContractMapper.toCertificateResponse(certificate);
    }

    @Transactional(readOnly = true)
    public V2Resources.CertificateResource getIssuerCertificate(UUID instanceId, UUID certificateId) {
        IssuerCertificate certificate = certificateService.getIssuerCertificate(certificateId, instanceId);
        return v2ContractMapper.toCertificateResponse(certificate);
    }

    public V2Resources.CertificateResource requestAccessCertificate(
        UUID instanceId,
        V2Resources.AccessCertificateRequest request
    ) {
        return v2ContractMapper.toCertificateResponse(
            certificateService.requestAccessCertificateForRelyingParty(instanceId, request.csr()));
    }

    public V2Resources.CertificateResource requestIssuerCertificate(
        UUID instanceId,
        V2Resources.IssuerCertificateRequest request
    ) {
        return v2ContractMapper.toCertificateResponse(
            certificateService.requestIssuerCertificate(instanceId, request.csr(), request.entitlement()));
    }

    public void revokeAccessCertificate(UUID instanceId, UUID certificateId) {
        certificateService.revokeAccessCertificate(certificateId, instanceId);
    }

    public void revokeIssuerCertificate(UUID instanceId, UUID certificateId) {
        certificateService.revokeIssuerCertificate(certificateId, instanceId);
    }

    private WalletRelyingParty findWalletRelyingParty(UUID id) {
        return walletRelyingPartyRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("WalletRelyingParty not found"));
    }

    private WalletRelyingPartyService findWalletRelyingPartyService(UUID id) {
        return walletRelyingPartyServiceRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("WalletRelyingPartyService not found"));
    }

    private WalletRelyingPartyService findActiveWalletRelyingPartyServiceForUpdate(UUID id) {
        WalletRelyingParty walletRelyingParty = walletRelyingPartyRepository.findByServiceIdForUpdate(id)
            .orElseThrow(() -> new NotFoundException("WalletRelyingPartyService not found"));
        if (!walletRelyingParty.isActive()) {
            throw new BadRequestException(
                "Cannot change intended uses for an inactive WalletRelyingParty");
        }
        WalletRelyingPartyService walletRelyingPartyService = findWalletRelyingPartyService(id);
        if (!walletRelyingPartyService.isActive()) {
            throw new BadRequestException(
                "Cannot change intended uses for an inactive WalletRelyingPartyService");
        }
        return walletRelyingPartyService;
    }

    private RelyingPartyInstance findRelyingPartyInstance(UUID id) {
        return relyingPartyInstanceRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("RelyingPartyInstance not found"));
    }

    private V2Resources.EntitlementResource toEntitlementResponse(Entitlement entitlement) {
        return new V2Resources.EntitlementResource(
            entitlement.getId(),
            entitlement.getEntitlement(),
            entitlement.isActive(),
            entitlement.getDisplayName(),
            entitlement.getCaId()
        );
    }

    private PageRequest pageRequest(
        int page,
        int pageSize,
        String sort,
        String defaultSort,
        Set<String> allowedSorts
    ) {
        if (page < 0 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new BadRequestException(
                "page must be non-negative and pageSize must be between 1 and " + MAX_PAGE_SIZE);
        }

        String sortProperty = sort == null ? defaultSort : sort;
        if (!allowedSorts.contains(sortProperty)) {
            throw new BadRequestException("Unsupported sort key");
        }
        return PageRequest.of(page, pageSize, Sort.by(sortProperty).ascending().and(Sort.by("id").ascending()));
    }
}
