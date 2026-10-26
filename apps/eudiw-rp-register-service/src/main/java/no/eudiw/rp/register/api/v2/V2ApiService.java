package no.eudiw.rp.register.api.v2;

import lombok.RequiredArgsConstructor;
import no.eudiw.rp.register.api.v2.endpoints.entities.AccessCertificateRequest;
import no.eudiw.rp.register.api.v2.endpoints.entities.CertificateResponse;
import no.eudiw.rp.register.api.v2.endpoints.entities.CertificatesResponse;
import no.eudiw.rp.register.api.v2.endpoints.entities.CreateIntendedUseRequest;
import no.eudiw.rp.register.api.v2.endpoints.entities.CreateRelyingPartyRequest;
import no.eudiw.rp.register.api.v2.endpoints.entities.EntitlementsResponse;
import no.eudiw.rp.register.api.v2.endpoints.entities.IntendedUseReplacementResponse;
import no.eudiw.rp.register.api.v2.endpoints.entities.IntendedUseResponse;
import no.eudiw.rp.register.api.v2.endpoints.entities.IssuerCertificateRequest;
import no.eudiw.rp.register.api.v2.endpoints.entities.RegistrationDetailsResponse;
import no.eudiw.rp.register.api.v2.endpoints.entities.RegistrationSummaryResponse;
import no.eudiw.rp.register.api.v2.endpoints.entities.RelyingPartyInstanceRequest;
import no.eudiw.rp.register.api.v2.endpoints.entities.RelyingPartyInstanceResponse;
import no.eudiw.rp.register.api.v2.endpoints.entities.RelyingPartyRequest;
import no.eudiw.rp.register.api.v2.endpoints.entities.RelyingPartyResponse;
import no.eudiw.rp.register.api.v2.endpoints.entities.RelyingPartyServiceRequest;
import no.eudiw.rp.register.api.v2.endpoints.entities.RelyingPartyServiceResponse;
import no.eudiw.rp.register.config.RegistryProperties;
import no.eudiw.rp.register.config.SupervisoryAuthorityProperties;
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
import no.eudiw.rp.register.repository.RegistrationSummaryRow;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(isolation = Isolation.READ_COMMITTED)
public class V2ApiService {

    private static final int MAX_PAGE_SIZE = 100;

    private final WalletRelyingPartyRepository walletRelyingPartyRepository;
    private final WalletRelyingPartyServiceRepository walletRelyingPartyServiceRepository;
    private final RelyingPartyInstanceRepository relyingPartyInstanceRepository;
    private final WalletRelyingPartyLookupService walletRelyingPartyLookupService;
    private final RegistryProperties registryProperties;
    private final SupervisoryAuthorityProperties supervisoryAuthorityProperties;
    private final EntitlementService entitlementService;
    private final RelyingPartyCertificateService certificateService;
    private final RelyingPartyService relyingPartyService;

    public RelyingPartyResponse createRelyingParty(
        CreateRelyingPartyRequest request
    ) {
        WalletRelyingParty relyingParty =
            walletRelyingPartyLookupService.createWalletRelyingParty(request.orgno());

        return RelyingPartyResponse.from(relyingParty, registryProperties, supervisoryAuthorityProperties);
    }

    public RelyingPartyResponse getRelyingParty(UUID relyingPartyId) {
        WalletRelyingParty relyingParty = findRelyingParty(relyingPartyId);
        return RelyingPartyResponse.from(relyingParty, registryProperties, supervisoryAuthorityProperties);
    }

    public Page<RelyingPartyResponse> searchRelyingParties(
        String orgno,
        int page,
        int pageSize,
        String sort
    ) {
        String defaultSort = "orgno";
        Map<String, String> allowedSortProperties = Map.of(
            "orgno", "orgno", "tradeName", "tradeName",
            "createdMs", "createdMs", "lastUpdatedMs", "lastUpdatedMs");

        PageRequest pageRequest = pageRequest(
            page, pageSize, sort, defaultSort, allowedSortProperties);
        Page<WalletRelyingParty> relyingParties = orgno == null
            ? walletRelyingPartyRepository.findAll(pageRequest)
            : walletRelyingPartyRepository.findAllByOrgno(orgno, pageRequest);

        return relyingParties.map(relyingParty ->
            RelyingPartyResponse.from(relyingParty, registryProperties, supervisoryAuthorityProperties));
    }

    public RelyingPartyResponse updateRelyingParty(
        UUID relyingPartyId,
        RelyingPartyRequest request
    ) {
        WalletRelyingParty relyingParty = walletRelyingPartyRepository.findByIdForUpdate(relyingPartyId)
            .orElseThrow(() -> new NotFoundException("RelyingParty not found"));

        String previousName = relyingParty.getName();
        boolean previousPSB = relyingParty.isPSB();
        request.update(relyingParty);
        if (!Objects.equals(previousName, relyingParty.getName()) || previousPSB != relyingParty.isPSB()) {
            relyingParty.getServices().stream()
                .flatMap(service -> service.getRelyingPartyInstances().stream())
                .forEach(RelyingPartyInstance::setUpdated);
        }

        return RelyingPartyResponse.from(
            walletRelyingPartyRepository.saveAndFlush(relyingParty),
            registryProperties,
            supervisoryAuthorityProperties);
    }

    public RelyingPartyServiceResponse createRelyingPartyService(
        UUID relyingPartyId,
        RelyingPartyServiceRequest request
    ) {
        WalletRelyingParty relyingParty = walletRelyingPartyRepository
            .findActiveByIdForUpdate(relyingPartyId)
            .orElseThrow(() -> new NotFoundException("RelyingParty not found or inactive"));

        WalletRelyingPartyService relyingPartyService = request.toDomain(relyingParty, List.of());
        relyingParty.getServices().add(relyingPartyService);

        return RelyingPartyServiceResponse.from(
            walletRelyingPartyServiceRepository.saveAndFlush(relyingPartyService));
    }

    public RelyingPartyServiceResponse getRelyingPartyService(UUID relyingPartyServiceId) {
        return RelyingPartyServiceResponse.from(findRelyingPartyService(relyingPartyServiceId));
    }

    public Page<RelyingPartyServiceResponse> searchRelyingPartyServices(
        UUID relyingPartyId,
        int page,
        int pageSize,
        String sort
    ) {
        if (relyingPartyId == null) {
            throw new BadRequestException("partyId is required");
        }

        Page<WalletRelyingPartyService> relyingPartyServices =
            walletRelyingPartyServiceRepository.findAllByWalletRelyingParty_Id(
                relyingPartyId,
                pageRequest(
                    page, pageSize, sort, "serviceTradeName",
                    Map.of("serviceTradeName", "serviceTradeName",
                        "createdMs", "createdMs", "lastUpdatedMs", "lastUpdatedMs")));

        return relyingPartyServices.map(RelyingPartyServiceResponse::from);
    }

    public RelyingPartyServiceResponse updateRelyingPartyService(
        UUID relyingPartyServiceId,
        RelyingPartyServiceRequest request
    ) {
        walletRelyingPartyRepository.findByServiceIdForUpdate(relyingPartyServiceId)
            .orElseThrow(() -> new NotFoundException("Service not found"));

        WalletRelyingPartyService relyingPartyService = findRelyingPartyService(relyingPartyServiceId);
        request.update(relyingPartyService);

        return RelyingPartyServiceResponse.from(
            walletRelyingPartyServiceRepository.saveAndFlush(relyingPartyService));
    }

    public RelyingPartyInstanceResponse createRelyingPartyInstance(
        UUID relyingPartyServiceId,
        RelyingPartyInstanceRequest request
    ) {
        walletRelyingPartyRepository
            .findByActiveServiceIdForUpdate(relyingPartyServiceId)
            .orElseThrow(() -> new NotFoundException("Service not found or inactive"));

        WalletRelyingPartyService relyingPartyServiceEntity = walletRelyingPartyServiceRepository
            .findActiveById(relyingPartyServiceId)
            .orElseThrow(() -> new NotFoundException("Service not found or inactive"));

        var relyingPartyEntitlements = request.toDomainRelyingPartyEntitlements();
        relyingPartyService.validateEntitlements(relyingPartyEntitlements);

        RelyingPartyInstance relyingPartyInstance = new RelyingPartyInstance(
            relyingPartyEntitlements,
            request.toDomainRelyingPartyEaas(),
            List.of()
        );

        relyingPartyInstance.setWalletRelyingPartyService(relyingPartyServiceEntity);
        relyingPartyServiceEntity.getRelyingPartyInstances().add(relyingPartyInstance);

        return RelyingPartyInstanceResponse.from(
            relyingPartyInstanceRepository.saveAndFlush(relyingPartyInstance));
    }

    public RelyingPartyInstanceResponse getRelyingPartyInstance(UUID relyingPartyInstanceId) {
        return RelyingPartyInstanceResponse.from(findRelyingPartyInstance(relyingPartyInstanceId));
    }

    @Transactional(readOnly = true)
    public RegistrationDetailsResponse getRegistrationDetails(UUID relyingPartyInstanceId) {
        RelyingPartyInstance relyingPartyInstance = findRelyingPartyInstance(relyingPartyInstanceId);
        Map<String, Entitlement> entitlementCatalog = entitlementService.findAllEntitlements(true).stream()
            .collect(Collectors.toMap(Entitlement::getEntitlement, Function.identity()));
        return RegistrationDetailsResponse.from(relyingPartyInstance, entitlementCatalog);
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public List<RegistrationSummaryResponse> getRegistrationsByOrgno(String orgno) {
        if (orgno == null || orgno.isBlank()) {
            throw new BadRequestException("Organization number is required");
        }

        List<RegistrationSummaryRow> rows = relyingPartyInstanceRepository.findRegistrationSummariesByOrgno(orgno);
        if (rows.isEmpty()) {
            return List.of();
        }

        var entitlements = entitlementService.findAllEntitlements(true).stream()
            .collect(Collectors.toMap(Entitlement::getEntitlement, Function.identity()));
        var registrations = rows.stream()
            .collect(Collectors.groupingBy(RegistrationSummaryRow::instanceId, LinkedHashMap::new, Collectors.toList()));

        return registrations.values().stream()
            .map(registration -> RegistrationSummaryResponse.from(registration, entitlements))
            .toList();
    }

    public Page<RelyingPartyInstanceResponse> searchRelyingPartyInstances(
        UUID relyingPartyServiceId,
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
            relyingPartyServiceId,
            orgno,
            searchTerm == null ? "" : searchTerm,
            includeInactive,
            uniqueRequiredEntitlements,
            uniqueRequiredEntitlements.size(),
            pageRequest(page, pageSize, sort, "createdMs", Map.of(
                "orgno", "walletRelyingPartyService.walletRelyingParty.orgno",
                "name", "walletRelyingPartyService.serviceTradeName",
                "serviceTradeName", "walletRelyingPartyService.serviceTradeName",
                "createdMs", "createdMs", "lastUpdatedMs", "lastUpdatedMs"))
        );

        return relyingPartyInstances.map(RelyingPartyInstanceResponse::from);
    }

    public RelyingPartyInstanceResponse updateRelyingPartyInstance(
        UUID relyingPartyInstanceId,
        RelyingPartyInstanceRequest request
    ) {
        walletRelyingPartyRepository.findByInstanceIdForUpdate(relyingPartyInstanceId)
            .orElseThrow(() -> new NotFoundException("RelyingPartyInstance not found"));

        RelyingPartyInstance relyingPartyInstance = findRelyingPartyInstance(relyingPartyInstanceId);
        List<RelyingPartyEntitlement> requestedEntitlements = request.toDomainRelyingPartyEntitlements();
        relyingPartyService.validateEntitlementsForUpdate(
            relyingPartyInstance.getRelyingPartyEntitlements(), requestedEntitlements);
        if (!relyingPartyInstance.canReplaceRelyingPartyEntitlements(requestedEntitlements)) {
            throw new BadRequestException(
                "An entitlement with issuer certificates cannot be removed or changed");
        }
        request.update(relyingPartyInstance, requestedEntitlements);

        return RelyingPartyInstanceResponse.from(
            relyingPartyInstanceRepository.saveAndFlush(relyingPartyInstance));
    }

    public List<IntendedUseResponse> getIntendedUses(UUID relyingPartyServiceId) {
        WalletRelyingPartyService relyingPartyService = findRelyingPartyService(relyingPartyServiceId);
        return relyingPartyService.getIntendedUses().stream()
            .map(IntendedUseResponse::from)
            .toList();
    }

    public IntendedUseResponse createIntendedUse(
        UUID relyingPartyServiceId,
        CreateIntendedUseRequest request
    ) {
        WalletRelyingPartyService relyingPartyService =
            findActiveRelyingPartyServiceForUpdate(relyingPartyServiceId);
        IntendedUse intendedUse = request.toDomain();
        intendedUse.setWalletRelyingPartyService(relyingPartyService);
        relyingPartyService.getIntendedUses().add(intendedUse);
        walletRelyingPartyServiceRepository.flush();

        return IntendedUseResponse.from(intendedUse);
    }

    public IntendedUseReplacementResponse replaceIntendedUse(
        UUID relyingPartyServiceId,
        UUID previousIntendedUseId,
        CreateIntendedUseRequest request
    ) {
        WalletRelyingPartyService relyingPartyService =
            findActiveRelyingPartyServiceForUpdate(relyingPartyServiceId);
        IntendedUse previousIntendedUse = relyingPartyService.getIntendedUses().stream()
            .filter(candidate -> previousIntendedUseId.equals(candidate.getId()))
            .findFirst()
            .orElseThrow(() -> new NotFoundException("IntendedUse not found"));

        if (previousIntendedUse.getRevokedAt() != null) {
            throw new BadRequestException("IntendedUse is already revoked");
        }

        String now = LocalDate.now(ZoneOffset.UTC).toString();
        IntendedUse currentIntendedUse = request.toDomain(now);
        previousIntendedUse.setRevokedAt(now);
        currentIntendedUse.setWalletRelyingPartyService(relyingPartyService);
        relyingPartyService.getIntendedUses().add(currentIntendedUse);
        walletRelyingPartyServiceRepository.flush();

        return IntendedUseReplacementResponse.from(previousIntendedUse, currentIntendedUse);
    }

    public IntendedUseResponse revokeIntendedUse(UUID relyingPartyServiceId, UUID intendedUseId) {
        WalletRelyingPartyService relyingPartyService =
            findActiveRelyingPartyServiceForUpdate(relyingPartyServiceId);
        IntendedUse intendedUse = relyingPartyService.getIntendedUses().stream()
            .filter(candidate -> intendedUseId.equals(candidate.getId()))
            .findFirst()
            .orElseThrow(() -> new NotFoundException("IntendedUse not found"));

        if (intendedUse.getRevokedAt() != null) {
            throw new BadRequestException("IntendedUse is already revoked");
        }

        intendedUse.setRevokedAt(LocalDate.now(ZoneOffset.UTC).toString());
        walletRelyingPartyServiceRepository.flush();

        return IntendedUseResponse.from(intendedUse);
    }

    @Transactional(readOnly = true)
    public EntitlementsResponse getEntitlements(boolean includeInactive) {
        return EntitlementsResponse.from(entitlementService.findAllEntitlements(includeInactive));
    }

    @Transactional(readOnly = true)
    public CertificatesResponse getAccessCertificates(UUID relyingPartyInstanceId) {
        List<CertificateResponse> certificates = certificateService
            .getCertificatesForRelyingParty(relyingPartyInstanceId)
            .stream().map(CertificateResponse::from).toList();
        return CertificatesResponse.from(certificates);
    }

    @Transactional(readOnly = true)
    public CertificatesResponse getIssuerCertificates(UUID relyingPartyInstanceId) {
        List<CertificateResponse> certificates = certificateService
            .getAllIssuerCertificatesFromRelyingParty(relyingPartyInstanceId)
            .stream().map(CertificateResponse::from).toList();
        return CertificatesResponse.from(certificates);
    }

    @Transactional(readOnly = true)
    public CertificateResponse getAccessCertificate(UUID relyingPartyInstanceId, UUID certificateId) {
        AccessCertificate certificate = certificateService.getCertificate(certificateId, relyingPartyInstanceId);
        return CertificateResponse.from(certificate);
    }

    @Transactional(readOnly = true)
    public CertificateResponse getIssuerCertificate(UUID relyingPartyInstanceId, UUID certificateId) {
        IssuerCertificate certificate =
            certificateService.getIssuerCertificate(certificateId, relyingPartyInstanceId);
        return CertificateResponse.from(certificate);
    }

    public CertificateResponse requestAccessCertificate(
        UUID relyingPartyInstanceId,
        AccessCertificateRequest request
    ) {
        return CertificateResponse.from(
            certificateService.requestAccessCertificateForRelyingParty(relyingPartyInstanceId, request.csr()));
    }

    public CertificateResponse requestIssuerCertificate(
        UUID relyingPartyInstanceId,
        IssuerCertificateRequest request
    ) {
        return CertificateResponse.from(
            certificateService.requestIssuerCertificate(
                relyingPartyInstanceId, request.csr(), request.entitlement()));
    }

    public void revokeAccessCertificate(UUID relyingPartyInstanceId, UUID certificateId) {
        certificateService.revokeAccessCertificate(certificateId, relyingPartyInstanceId);
    }

    public void revokeIssuerCertificate(UUID relyingPartyInstanceId, UUID certificateId) {
        certificateService.revokeIssuerCertificate(certificateId, relyingPartyInstanceId);
    }

    private WalletRelyingParty findRelyingParty(UUID relyingPartyId) {
        return walletRelyingPartyRepository.findById(relyingPartyId)
            .orElseThrow(() -> new NotFoundException("Party not found"));
    }

    private WalletRelyingPartyService findRelyingPartyService(UUID relyingPartyServiceId) {
        return walletRelyingPartyServiceRepository.findById(relyingPartyServiceId)
            .orElseThrow(() -> new NotFoundException("Service not found"));
    }

    private WalletRelyingPartyService findActiveRelyingPartyServiceForUpdate(UUID relyingPartyServiceId) {
        walletRelyingPartyRepository.findByActiveServiceIdForUpdate(relyingPartyServiceId)
            .orElseThrow(() -> new NotFoundException("Service not found or inactive"));

        return walletRelyingPartyServiceRepository.findActiveById(relyingPartyServiceId)
            .orElseThrow(() -> new NotFoundException("Service not found or inactive"));
    }

    private RelyingPartyInstance findRelyingPartyInstance(UUID relyingPartyInstanceId) {
        return relyingPartyInstanceRepository.findById(relyingPartyInstanceId)
            .orElseThrow(() -> new NotFoundException("RelyingPartyInstance not found"));
    }

    private PageRequest pageRequest(
        int page,
        int pageSize,
        String sort,
        String defaultSort,
        Map<String, String> allowedSorts
    ) {
        if (page < 0 || pageSize < 1 || pageSize > MAX_PAGE_SIZE
            || (long) page * pageSize > Integer.MAX_VALUE) {
            throw new BadRequestException(
                "page must be non-negative, pageSize must be between 1 and " + MAX_PAGE_SIZE
                    + ", and page * pageSize must not exceed " + Integer.MAX_VALUE);
        }

        String sortKey = sort == null ? defaultSort : sort;
        String sortProperty = allowedSorts.get(sortKey);
        if (sortProperty == null) {
            throw new BadRequestException("Unsupported sort key");
        }
        return PageRequest.of(page, pageSize, Sort.by(sortProperty).ascending().and(Sort.by("id").ascending()));
    }
}
