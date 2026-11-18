package no.eudiw.rp.register.api.v2;

import lombok.RequiredArgsConstructor;
import no.eudiw.rp.register.api.v2.endpoints.entities.AccessCertificateRequest;
import no.eudiw.rp.register.api.v2.endpoints.entities.CertificateResponse;
import no.eudiw.rp.register.api.v2.endpoints.entities.CertificatesResponse;
import no.eudiw.rp.register.api.v2.endpoints.entities.CreateIntendedUseRequest;
import no.eudiw.rp.register.api.v2.endpoints.entities.CreateInstanceRequest;
import no.eudiw.rp.register.api.v2.endpoints.entities.CreateRelyingPartyRequest;
import no.eudiw.rp.register.api.v2.endpoints.entities.EntitlementsResponse;
import no.eudiw.rp.register.api.v2.endpoints.entities.IntendedUseReplacementResponse;
import no.eudiw.rp.register.api.v2.endpoints.entities.IntendedUseResponse;
import no.eudiw.rp.register.api.v2.endpoints.entities.IssuerCertificateRequest;
import no.eudiw.rp.register.api.v2.endpoints.entities.RegistrationDetailsResponse;
import no.eudiw.rp.register.api.v2.endpoints.entities.RegistrationSummaryResponse;
import no.eudiw.rp.register.api.v2.endpoints.entities.RelyingPartyEaasRequest;
import no.eudiw.rp.register.api.v2.endpoints.entities.RelyingPartyEntitlementsRequest;
import no.eudiw.rp.register.api.v2.endpoints.entities.RelyingPartyInstanceRequest;
import no.eudiw.rp.register.api.v2.endpoints.entities.RelyingPartyInstanceResponse;
import no.eudiw.rp.register.api.v2.endpoints.entities.RelyingPartyRequest;
import no.eudiw.rp.register.api.v2.endpoints.entities.RelyingPartyInformationRequest;
import no.eudiw.rp.register.api.v2.endpoints.entities.RelyingPartyResponse;
import no.eudiw.rp.register.api.v2.endpoints.entities.RelyingPartyServiceInformationRequest;
import no.eudiw.rp.register.api.v2.endpoints.entities.RelyingPartyServiceEntitlementsRequest;
import no.eudiw.rp.register.api.v2.endpoints.entities.RelyingPartyServiceRequest;
import no.eudiw.rp.register.api.v2.endpoints.entities.RelyingPartyServiceResponse;
import no.eudiw.rp.register.api.v2.resource.RelyingPartyEntitlementResource;
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
import java.util.ArrayList;
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

    public RelyingPartyInstanceResponse createInstance(CreateInstanceRequest request) {
        List<RelyingPartyEntitlement> entitlements = request.relyingPartyEntitlements().stream()
            .map(item -> item.toDomain())
            .toList();
        RelyingPartyInstance relyingPartyInstance = relyingPartyService.createRelyingParty(
            request.orgno(), request.serviceTradeName(), entitlements, List.of());

        return RelyingPartyInstanceResponse.from(relyingPartyInstance);
    }

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

    public RelyingPartyResponse updateRelyingPartyInformation(
        UUID relyingPartyId,
        RelyingPartyInformationRequest request
    ) {
        WalletRelyingParty relyingParty = walletRelyingPartyRepository.findByIdForUpdate(relyingPartyId)
            .orElseThrow(() -> new NotFoundException("RelyingParty not found"));

        String tradeName = request.tradeName();
        relyingParty.setTradeName(tradeName == null || tradeName.isBlank() ? null : tradeName.strip());
        relyingParty.setCountry(request.country());
        if (request.postalAddress() != null) {
            relyingParty.setPostalAddress(new ArrayList<>(request.postalAddress()));
        }
        if (request.email() != null) {
            relyingParty.setEmail(new ArrayList<>(request.email()));
        }
        if (request.phone() != null) {
            relyingParty.setPhone(new ArrayList<>(request.phone()));
        }
        if (request.infoURI() != null) {
            relyingParty.setInfoURI(new ArrayList<>(request.infoURI()));
        }
        relyingParty.setUpdated();

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
        if (request.entitlements() != null) {
            this.relyingPartyService.validateServiceEntitlements(relyingPartyService, request.entitlements());
            relyingPartyService.setEntitlements(request.entitlements());
        }

        relyingParty.getServices().add(relyingPartyService);

        return RelyingPartyServiceResponse.from(
            walletRelyingPartyServiceRepository.saveAndFlush(relyingPartyService));
    }

    public RelyingPartyServiceResponse getRelyingPartyService(UUID relyingPartyServiceId) {
        return RelyingPartyServiceResponse.from(findRelyingPartyService(relyingPartyServiceId));
    }

    @Transactional(readOnly = true)
    public List<RelyingPartyEntitlementResource> getCredentialIssuerUrls(UUID serviceId) {
        WalletRelyingPartyService service = findRelyingPartyService(serviceId);
        return service.getEntitlements().stream()
            .map(role -> new RelyingPartyEntitlementResource(
                role, service.getCredentialIssuerUrl(role)))
            .toList();
    }

    public List<RelyingPartyEntitlementResource> patchCredentialIssuerUrls(
        UUID serviceId, List<RelyingPartyEntitlementResource> request
    ) {
        walletRelyingPartyRepository.findByServiceIdForUpdate(serviceId)
            .orElseThrow(() -> new NotFoundException("Service not found"));
        WalletRelyingPartyService service = findRelyingPartyService(serviceId);

        long distinctRoles = request.stream().map(RelyingPartyEntitlementResource::entitlement).distinct().count();

        if (distinctRoles != request.size()) {
            throw new BadRequestException("Service roles must be unique");
        }

        request.forEach(item -> service.requireServiceEntitlement(item.entitlement()));
        request.stream().filter(item -> item.credentialIssuerUrlPresent())
            .forEach(item -> service.setCredentialIssuerUrl(item.entitlement(), item.credentialIssuerUrl()));
        walletRelyingPartyServiceRepository.saveAndFlush(service);

        return getCredentialIssuerUrls(serviceId);
    }

    public RelyingPartyServiceResponse patchServiceEntitlements(
        UUID serviceId, RelyingPartyServiceEntitlementsRequest request
    ) {
        walletRelyingPartyRepository.findByServiceIdForUpdate(serviceId)
            .orElseThrow(() -> new NotFoundException("Service not found"));
        WalletRelyingPartyService service = findRelyingPartyService(serviceId);
        Set<String> requestedEntitlements = new HashSet<>(request.entitlements());

        if (requestedEntitlements.size() != request.entitlements().size()) {
            throw new BadRequestException("Service roles must be unique");
        }

        List<RelyingPartyEntitlementResource> credentialIssuerUrls = request.credentialIssuerUrls() == null
            ? List.of()
            : request.credentialIssuerUrls();
        long distinctUrlEntitlements = credentialIssuerUrls.stream()
            .map(RelyingPartyEntitlementResource::entitlement).distinct().count();

        if (distinctUrlEntitlements != credentialIssuerUrls.size()) {
            throw new BadRequestException("Service URL roles must be unique");
        }

        for (RelyingPartyEntitlementResource credentialIssuerUrl : credentialIssuerUrls) {
            if (!requestedEntitlements.contains(credentialIssuerUrl.entitlement())) {
                throw new BadRequestException("The role is not in the service role list");
            }
        }

        relyingPartyService.validateServiceEntitlements(service, request.entitlements());
        service.setEntitlements(request.entitlements());
        credentialIssuerUrls.stream().filter(RelyingPartyEntitlementResource::credentialIssuerUrlPresent)
            .forEach(item -> service.setCredentialIssuerUrl(item.entitlement(), item.credentialIssuerUrl()));

        return RelyingPartyServiceResponse.from(walletRelyingPartyServiceRepository.saveAndFlush(service));
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

        if (request.entitlements() != null) {
            this.relyingPartyService.validateServiceEntitlements(relyingPartyService, request.entitlements());
            relyingPartyService.setEntitlements(request.entitlements());
        }

        request.update(relyingPartyService);

        return RelyingPartyServiceResponse.from(
            walletRelyingPartyServiceRepository.saveAndFlush(relyingPartyService));
    }

    public RelyingPartyServiceResponse updateRelyingPartyServiceInformation(
        UUID relyingPartyServiceId,
        RelyingPartyServiceInformationRequest request
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
        relyingPartyEntitlements.forEach(selection ->
            relyingPartyServiceEntity.requireServiceEntitlement(selection.getEntitlement()));
        relyingPartyService.validateEntitlements(relyingPartyEntitlements);
        validateCredentialIssuerUrls(relyingPartyServiceEntity, request.relyingPartyEntitlements());

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
        Map<String, Entitlement> entitlementCatalog = entitlementService.findAllEntitlements(true)
            .stream()
            .collect(Collectors.toMap(Entitlement::getEntitlement, Function.identity()));

        return RegistrationDetailsResponse.from(
            relyingPartyInstance, entitlementCatalog, registryProperties, supervisoryAuthorityProperties);
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
        boolean hideSyntheticOrgnos,
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
            hideSyntheticOrgnos,
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
        RelyingPartyInstance relyingPartyInstance = findRelyingPartyInstanceForUpdate(relyingPartyInstanceId);
        List<RelyingPartyEntitlement> requestedEntitlements = request.toDomainRelyingPartyEntitlements();
        validateCredentialIssuerUrls(
            relyingPartyInstance.getWalletRelyingPartyService(), request.relyingPartyEntitlements());
        validateInstanceEntitlements(relyingPartyInstance, requestedEntitlements);
        request.update(relyingPartyInstance, requestedEntitlements);

        return RelyingPartyInstanceResponse.from(
            relyingPartyInstanceRepository.saveAndFlush(relyingPartyInstance));
    }

    public RelyingPartyInstanceResponse updateRelyingPartyInstanceEaas(
        UUID relyingPartyInstanceId,
        RelyingPartyEaasRequest request
    ) {
        RelyingPartyInstance relyingPartyInstance = findRelyingPartyInstanceForUpdate(relyingPartyInstanceId);
        relyingPartyInstance.setRelyingPartyEaas(request.toDomain());
        relyingPartyInstance.setUpdated();

        return RelyingPartyInstanceResponse.from(
            relyingPartyInstanceRepository.saveAndFlush(relyingPartyInstance));
    }

    public RelyingPartyInstanceResponse updateRelyingPartyInstanceEntitlements(
        UUID relyingPartyInstanceId,
        RelyingPartyEntitlementsRequest request
    ) {
        RelyingPartyInstance relyingPartyInstance = findRelyingPartyInstanceForUpdate(relyingPartyInstanceId);
        List<RelyingPartyEntitlement> requestedEntitlements = request.toDomain();
        validateCredentialIssuerUrls(
            relyingPartyInstance.getWalletRelyingPartyService(), request.relyingPartyEntitlements());
        validateInstanceEntitlements(relyingPartyInstance, requestedEntitlements);
        relyingPartyInstance.setRelyingPartyEntitlements(requestedEntitlements);
        relyingPartyInstance.setUpdated();

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

    private RelyingPartyInstance findRelyingPartyInstanceForUpdate(UUID relyingPartyInstanceId) {
        walletRelyingPartyRepository.findByInstanceIdForUpdate(relyingPartyInstanceId)
            .orElseThrow(() -> new NotFoundException("RelyingPartyInstance not found"));

        return findRelyingPartyInstance(relyingPartyInstanceId);
    }

    private void validateCredentialIssuerUrls(
        WalletRelyingPartyService service,
        List<RelyingPartyEntitlementResource> request
    ) {
        request.stream().filter(item -> item.credentialIssuerUrlPresent())
            .forEach(item -> relyingPartyService.validateCredentialIssuerUrl(
                service, item.entitlement(), item.credentialIssuerUrl()));
    }

    private void validateInstanceEntitlements(
        RelyingPartyInstance relyingPartyInstance,
        List<RelyingPartyEntitlement> requestedEntitlements
    ) {
        requestedEntitlements.forEach(selection -> relyingPartyInstance.getWalletRelyingPartyService()
            .requireServiceEntitlement(selection.getEntitlement()));
        relyingPartyService.validateEntitlementsForUpdate(
            relyingPartyInstance.getRelyingPartyEntitlements(), requestedEntitlements);

        if (!relyingPartyInstance.keepsEntitlementsWithIssuerCertificates(requestedEntitlements)) {
            throw new BadRequestException("An entitlement with issuer certificates cannot be removed");
        }
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
