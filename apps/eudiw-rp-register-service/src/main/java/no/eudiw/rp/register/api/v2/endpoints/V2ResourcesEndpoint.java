package no.eudiw.rp.register.api.v2.endpoints;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import no.eudiw.rp.register.api.v2.V2ApiService;
import no.eudiw.rp.register.api.v2.endpoints.entities.AccessCertificateRequest;
import no.eudiw.rp.register.api.v2.endpoints.entities.CertificateResponse;
import no.eudiw.rp.register.api.v2.endpoints.entities.CertificatesResponse;
import no.eudiw.rp.register.api.v2.endpoints.entities.CreateIntendedUseRequest;
import no.eudiw.rp.register.api.v2.endpoints.entities.CreateWalletRelyingPartyRequest;
import no.eudiw.rp.register.api.v2.endpoints.entities.EntitlementsResponse;
import no.eudiw.rp.register.api.v2.endpoints.entities.IntendedUseReplacementResponse;
import no.eudiw.rp.register.api.v2.endpoints.entities.IntendedUseResponse;
import no.eudiw.rp.register.api.v2.endpoints.entities.IssuerCertificateRequest;
import no.eudiw.rp.register.api.v2.endpoints.entities.RelyingPartyInstanceRequest;
import no.eudiw.rp.register.api.v2.endpoints.entities.RelyingPartyInstanceResponse;
import no.eudiw.rp.register.api.v2.endpoints.entities.WalletRelyingPartyRequest;
import no.eudiw.rp.register.api.v2.endpoints.entities.WalletRelyingPartyResponse;
import no.eudiw.rp.register.api.v2.endpoints.entities.WalletRelyingPartyServiceRequest;
import no.eudiw.rp.register.api.v2.endpoints.entities.WalletRelyingPartyServiceResponse;
import no.idporten.logging.audit.Audit;
import no.idporten.validators.orgnr.Orgnr;
import org.springframework.data.web.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(
    name = "registration-v2-api",
    description = "Internal registration API"
)
@Validated
@RestController
@RequestMapping("/v2")
@RequiredArgsConstructor
public class V2ResourcesEndpoint {

    private static final String AUDIT_WALLET_RELYING_PARTY_CREATED = "V2-WALLET-RELYING-PARTY-CREATED";
    private static final String AUDIT_WALLET_RELYING_PARTY_UPDATED = "V2-WALLET-RELYING-PARTY-UPDATED";
    private static final String AUDIT_WALLET_RELYING_PARTY_SERVICE_CREATED =
        "V2-WALLET-RELYING-PARTY-SERVICE-CREATED";
    private static final String AUDIT_WALLET_RELYING_PARTY_SERVICE_UPDATED =
        "V2-WALLET-RELYING-PARTY-SERVICE-UPDATED";
    private static final String AUDIT_RELYING_PARTY_INSTANCE_CREATED = "V2-RELYING-PARTY-INSTANCE-CREATED";
    private static final String AUDIT_RELYING_PARTY_INSTANCE_UPDATED = "V2-RELYING-PARTY-INSTANCE-UPDATED";
    private static final String AUDIT_INTENDED_USE_CREATED = "V2-INTENDED-USE-CREATED";
    private static final String AUDIT_INTENDED_USE_REVOKED = "V2-INTENDED-USE-REVOKED";
    private static final String AUDIT_INTENDED_USE_REPLACED = "V2-INTENDED-USE-REPLACED";
    private static final String AUDIT_ACCESS_CERTIFICATE_CREATED = "V2-ACCESS-CERTIFICATE-CREATED";
    private static final String AUDIT_ISSUER_CERTIFICATE_CREATED = "V2-ISSUER-CERTIFICATE-CREATED";
    private static final String AUDIT_ACCESS_CERTIFICATE_REVOKED = "V2-ACCESS-CERTIFICATE-REVOKED";
    private static final String AUDIT_ISSUER_CERTIFICATE_REVOKED = "V2-ISSUER-CERTIFICATE-REVOKED";

    private final V2ApiService v2ApiService;

    @Operation(
        summary = "Create a WalletRelyingParty",
        description = "Returns 400 if the organization number already exists; find it with GET /v2/parties?orgno={orgno}."
    )
    @Audit(auditId = AUDIT_WALLET_RELYING_PARTY_CREATED)
    @PostMapping(path = "/parties", consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WalletRelyingPartyResponse> createWalletRelyingParty(
        @Valid @RequestBody CreateWalletRelyingPartyRequest request
    ) {
        return ResponseEntity.ok(v2ApiService.createWalletRelyingParty(request));
    }

    @Operation(summary = "Get a WalletRelyingParty")
    @GetMapping(path = "/parties/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public WalletRelyingPartyResponse getWalletRelyingParty(@PathVariable UUID id) {
        return v2ApiService.getWalletRelyingParty(id);
    }

    @Operation(summary = "Find by organization number or list WalletRelyingParties")
    @GetMapping(path = "/parties", produces = MediaType.APPLICATION_JSON_VALUE)
    public PagedModel<WalletRelyingPartyResponse> searchWalletRelyingParties(
        @Orgnr
        @RequestParam(required = false) String orgno,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "25") int pageSize,
        @RequestParam(required = false) String sort
    ) {
        return new PagedModel<>(v2ApiService.searchWalletRelyingParties(orgno, page, pageSize, sort));
    }

    @Operation(
        summary = "Replace editable WalletRelyingParty properties",
        description = "Replaces editable properties only; omitted optional properties are cleared and relations preserved."
    )
    @Audit(auditId = AUDIT_WALLET_RELYING_PARTY_UPDATED)
    @PutMapping(path = "/parties/{id}", consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public WalletRelyingPartyResponse updateWalletRelyingParty(
        @PathVariable UUID id,
        @Valid @RequestBody WalletRelyingPartyRequest request
    ) {
        return v2ApiService.updateWalletRelyingParty(id, request);
    }

    @Operation(summary = "Create a WalletRelyingPartyService")
    @Audit(auditId = AUDIT_WALLET_RELYING_PARTY_SERVICE_CREATED)
    @PostMapping(path = "/parties/{partyId}/services", consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WalletRelyingPartyServiceResponse> createWalletRelyingPartyService(
        @Parameter(description = "Parent WalletRelyingParty identifier")
        @PathVariable UUID partyId,
        @Valid @RequestBody WalletRelyingPartyServiceRequest request
    ) {
        return ResponseEntity.ok(v2ApiService.createWalletRelyingPartyService(partyId, request));
    }

    @Operation(summary = "Get a WalletRelyingPartyService")
    @GetMapping(path = "/services/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public WalletRelyingPartyServiceResponse getWalletRelyingPartyService(@PathVariable UUID id) {
        return v2ApiService.getWalletRelyingPartyService(id);
    }

    @Operation(summary = "List WalletRelyingPartyServices")
    @GetMapping(path = "/services", produces = MediaType.APPLICATION_JSON_VALUE)
    public PagedModel<WalletRelyingPartyServiceResponse> searchWalletRelyingPartyServices(
        @Parameter(description = "Owning WalletRelyingParty identifier")
        @RequestParam UUID partyId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "25") int pageSize,
        @RequestParam(required = false) String sort
    ) {
        return new PagedModel<>(v2ApiService.searchWalletRelyingPartyServices(partyId, page, pageSize, sort));
    }

    @Operation(
        summary = "Replace editable WalletRelyingPartyService properties",
        description = "Replaces editable properties only; omitted optional properties are cleared and relations preserved."
    )
    @Audit(auditId = AUDIT_WALLET_RELYING_PARTY_SERVICE_UPDATED)
    @PutMapping(path = "/services/{id}", consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public WalletRelyingPartyServiceResponse updateWalletRelyingPartyService(
        @PathVariable UUID id,
        @Valid @RequestBody WalletRelyingPartyServiceRequest request
    ) {
        return v2ApiService.updateWalletRelyingPartyService(id, request);
    }

    @Operation(summary = "Create a RelyingPartyInstance")
    @Audit(auditId = AUDIT_RELYING_PARTY_INSTANCE_CREATED)
    @PostMapping(path = "/services/{serviceId}/instances", consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RelyingPartyInstanceResponse> createRelyingPartyInstance(
        @Parameter(description = "Parent WalletRelyingPartyService identifier")
        @PathVariable UUID serviceId,
        @Valid @RequestBody RelyingPartyInstanceRequest request
    ) {
        return ResponseEntity.ok(v2ApiService.createRelyingPartyInstance(serviceId, request));
    }

    @Operation(summary = "Get a RelyingPartyInstance")
    @GetMapping(path = "/instances/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public RelyingPartyInstanceResponse getRelyingPartyInstance(@PathVariable UUID id) {
        return v2ApiService.getRelyingPartyInstance(id);
    }

    @Operation(summary = "Search RelyingPartyInstances")
    @GetMapping(path = "/instances", produces = MediaType.APPLICATION_JSON_VALUE)
    public PagedModel<RelyingPartyInstanceResponse> searchRelyingPartyInstances(
        @Parameter(description = "Optional WalletRelyingPartyService identifier")
        @RequestParam(required = false) UUID serviceId,
        @Orgnr
        @RequestParam(required = false) String orgno,
        @RequestParam(required = false) String searchTerm,
        @RequestParam(defaultValue = "false") boolean includeInactive,
        @RequestParam(required = false) List<@NotBlank String> requiredEntitlements,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "25") int pageSize,
        @RequestParam(required = false) String sort
    ) {
        return new PagedModel<>(v2ApiService.searchRelyingPartyInstances(
            serviceId, orgno, searchTerm, includeInactive, requiredEntitlements, page, pageSize, sort));
    }

    @Operation(
        summary = "Replace editable RelyingPartyInstance properties",
        description = "Replaces entitlements and attestations only; parent, status and certificates are preserved."
    )
    @Audit(auditId = AUDIT_RELYING_PARTY_INSTANCE_UPDATED)
    @PutMapping(path = "/instances/{id}", consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public RelyingPartyInstanceResponse updateRelyingPartyInstance(
        @PathVariable UUID id,
        @Valid @RequestBody RelyingPartyInstanceRequest request
    ) {
        return v2ApiService.updateRelyingPartyInstance(id, request);
    }

    @Operation(summary = "List IntendedUse snapshots")
    @GetMapping(path = "/services/{serviceId}/intended-uses", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<IntendedUseResponse> getIntendedUses(
        @Parameter(description = "Owning WalletRelyingPartyService identifier")
        @PathVariable UUID serviceId
    ) {
        return v2ApiService.getIntendedUses(serviceId);
    }

    @Operation(summary = "Create an IntendedUse snapshot")
    @Audit(auditId = AUDIT_INTENDED_USE_CREATED)
    @PostMapping(path = "/services/{serviceId}/intended-uses", consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<IntendedUseResponse> createIntendedUse(
        @Parameter(description = "Owning WalletRelyingPartyService identifier")
        @PathVariable UUID serviceId,
        @Valid @RequestBody CreateIntendedUseRequest request
    ) {
        return ResponseEntity.ok(v2ApiService.createIntendedUse(serviceId, request));
    }

    @Operation(summary = "Replace an IntendedUse snapshot")
    @Audit(auditId = AUDIT_INTENDED_USE_REPLACED)
    @PostMapping(path = "/services/{serviceId}/intended-uses/{id}/replace",
        consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<IntendedUseReplacementResponse> replaceIntendedUse(
        @Parameter(description = "Owning WalletRelyingPartyService identifier")
        @PathVariable UUID serviceId,
        @Parameter(description = "IntendedUse identifier to replace")
        @PathVariable UUID id,
        @Valid @RequestBody CreateIntendedUseRequest request
    ) {
        return ResponseEntity.ok(v2ApiService.replaceIntendedUse(serviceId, id, request));
    }

    @Operation(summary = "Revoke an IntendedUse snapshot")
    @Audit(auditId = AUDIT_INTENDED_USE_REVOKED)
    @PatchMapping(path = "/services/{serviceId}/intended-uses/{id}/revoke",
        produces = MediaType.APPLICATION_JSON_VALUE)
    public IntendedUseResponse revokeIntendedUse(
        @Parameter(description = "Owning WalletRelyingPartyService identifier")
        @PathVariable UUID serviceId,
        @Parameter(description = "IntendedUse identifier to revoke")
        @PathVariable UUID id
    ) {
        return v2ApiService.revokeIntendedUse(serviceId, id);
    }

    @Operation(summary = "List entitlements")
    @GetMapping(path = "/entitlements", produces = MediaType.APPLICATION_JSON_VALUE)
    public EntitlementsResponse getEntitlements(
        @RequestParam(defaultValue = "false") boolean includeInactive
    ) {
        return v2ApiService.getEntitlements(includeInactive);
    }

    @Operation(summary = "List access certificates")
    @GetMapping(path = "/instances/{instanceId}/certificates/access",
        produces = MediaType.APPLICATION_JSON_VALUE)
    public CertificatesResponse getAccessCertificates(@PathVariable UUID instanceId) {
        return v2ApiService.getAccessCertificates(instanceId);
    }

    @Operation(summary = "List issuer certificates")
    @GetMapping(path = "/instances/{instanceId}/certificates/issuer",
        produces = MediaType.APPLICATION_JSON_VALUE)
    public CertificatesResponse getIssuerCertificates(@PathVariable UUID instanceId) {
        return v2ApiService.getIssuerCertificates(instanceId);
    }

    @Operation(summary = "Get an access certificate")
    @GetMapping(path = "/instances/{instanceId}/certificates/access/{certificateId}",
        produces = MediaType.APPLICATION_JSON_VALUE)
    public CertificateResponse getAccessCertificate(
        @PathVariable UUID instanceId,
        @PathVariable UUID certificateId
    ) {
        return v2ApiService.getAccessCertificate(instanceId, certificateId);
    }

    @Operation(summary = "Get an issuer certificate")
    @GetMapping(path = "/instances/{instanceId}/certificates/issuer/{certificateId}",
        produces = MediaType.APPLICATION_JSON_VALUE)
    public CertificateResponse getIssuerCertificate(
        @PathVariable UUID instanceId,
        @PathVariable UUID certificateId
    ) {
        return v2ApiService.getIssuerCertificate(instanceId, certificateId);
    }

    @Operation(summary = "Request an access certificate")
    @Audit(auditId = AUDIT_ACCESS_CERTIFICATE_CREATED)
    @PostMapping(path = "/instances/{instanceId}/certificates/access",
        consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public CertificateResponse requestAccessCertificate(
        @PathVariable UUID instanceId,
        @Valid @RequestBody AccessCertificateRequest request
    ) {
        return v2ApiService.requestAccessCertificate(instanceId, request);
    }

    @Operation(summary = "Request an issuer certificate")
    @Audit(auditId = AUDIT_ISSUER_CERTIFICATE_CREATED)
    @PostMapping(path = "/instances/{instanceId}/certificates/issuer",
        consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public CertificateResponse requestIssuerCertificate(
        @PathVariable UUID instanceId,
        @Valid @RequestBody IssuerCertificateRequest request
    ) {
        return v2ApiService.requestIssuerCertificate(instanceId, request);
    }

    @Operation(summary = "Revoke an access certificate")
    @Audit(auditId = AUDIT_ACCESS_CERTIFICATE_REVOKED)
    @PatchMapping(path = "/instances/{instanceId}/certificates/access/{certificateId}/revoke")
    public ResponseEntity<Void> revokeAccessCertificate(
        @PathVariable UUID instanceId,
        @PathVariable UUID certificateId
    ) {
        v2ApiService.revokeAccessCertificate(instanceId, certificateId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Revoke an issuer certificate")
    @Audit(auditId = AUDIT_ISSUER_CERTIFICATE_REVOKED)
    @PatchMapping(path = "/instances/{instanceId}/certificates/issuer/{certificateId}/revoke")
    public ResponseEntity<Void> revokeIssuerCertificate(
        @PathVariable UUID instanceId,
        @PathVariable UUID certificateId
    ) {
        v2ApiService.revokeIssuerCertificate(instanceId, certificateId);
        return ResponseEntity.noContent().build();
    }
}
