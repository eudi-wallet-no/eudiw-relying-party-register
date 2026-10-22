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
import no.eudiw.rp.register.api.v2.endpoints.entities.CreateRelyingPartyRequest;
import no.eudiw.rp.register.api.v2.endpoints.entities.EntitlementsResponse;
import no.eudiw.rp.register.api.v2.endpoints.entities.IntendedUseReplacementResponse;
import no.eudiw.rp.register.api.v2.endpoints.entities.IntendedUseResponse;
import no.eudiw.rp.register.api.v2.endpoints.entities.IssuerCertificateRequest;
import no.eudiw.rp.register.api.v2.endpoints.entities.RegistrationSummaryResponse;
import no.eudiw.rp.register.api.v2.endpoints.entities.RelyingPartyInstanceRequest;
import no.eudiw.rp.register.api.v2.endpoints.entities.RelyingPartyInstanceResponse;
import no.eudiw.rp.register.api.v2.endpoints.entities.RelyingPartyRequest;
import no.eudiw.rp.register.api.v2.endpoints.entities.RelyingPartyResponse;
import no.eudiw.rp.register.api.v2.endpoints.entities.RelyingPartyServiceRequest;
import no.eudiw.rp.register.api.v2.endpoints.entities.RelyingPartyServiceResponse;
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

    private static final String AUDIT_PARTY_CREATED = "V2-WALLET-RELYING-PARTY-CREATED";
    private static final String AUDIT_PARTY_UPDATED = "V2-WALLET-RELYING-PARTY-UPDATED";
    private static final String AUDIT_SERVICE_CREATED =
        "V2-WALLET-RELYING-PARTY-SERVICE-CREATED";
    private static final String AUDIT_SERVICE_UPDATED =
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
        summary = "Create a RelyingParty",
        description = "Returns 400 if the organization number already exists; find it with GET /v2/parties?orgno={orgno}."
    )
    @Audit(auditId = AUDIT_PARTY_CREATED)
    @PostMapping(path = "/parties", consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RelyingPartyResponse> createRelyingParty(
        @Valid @RequestBody CreateRelyingPartyRequest request
    ) {
        return ResponseEntity.ok(v2ApiService.createRelyingParty(request));
    }

    @Operation(summary = "Get a RelyingParty")
    @GetMapping(path = "/parties/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public RelyingPartyResponse getRelyingParty(@PathVariable("id") UUID relyingPartyId) {
        return v2ApiService.getRelyingParty(relyingPartyId);
    }

    @Operation(summary = "Find by organization number or list parties")
    @GetMapping(path = "/parties", produces = MediaType.APPLICATION_JSON_VALUE)
    public PagedModel<RelyingPartyResponse> searchRelyingParties(
        @Orgnr
        @RequestParam(required = false) String orgno,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "25") int pageSize,
        @RequestParam(required = false) String sort
    ) {
        return new PagedModel<>(v2ApiService.searchRelyingParties(orgno, page, pageSize, sort));
    }

    @Operation(
        summary = "Replace editable party properties",
        description = "Replaces editable properties only; omitted optional properties are cleared and relations preserved."
    )
    @Audit(auditId = AUDIT_PARTY_UPDATED)
    @PutMapping(path = "/parties/{id}", consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public RelyingPartyResponse updateRelyingParty(
        @PathVariable("id") UUID relyingPartyId,
        @Valid @RequestBody RelyingPartyRequest request
    ) {
        return v2ApiService.updateRelyingParty(relyingPartyId, request);
    }

    @Operation(summary = "Create a RelyingPartyService")
    @Audit(auditId = AUDIT_SERVICE_CREATED)
    @PostMapping(path = "/parties/{partyId}/services", consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RelyingPartyServiceResponse> createRelyingPartyService(
        @Parameter(description = "Parent RelyingParty identifier")
        @PathVariable("partyId") UUID relyingPartyId,
        @Valid @RequestBody RelyingPartyServiceRequest request
    ) {
        return ResponseEntity.ok(v2ApiService.createRelyingPartyService(relyingPartyId, request));
    }

    @Operation(summary = "Get a RelyingPartyService")
    @GetMapping(path = "/services/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public RelyingPartyServiceResponse getRelyingPartyService(
        @PathVariable("id") UUID relyingPartyServiceId
    ) {
        return v2ApiService.getRelyingPartyService(relyingPartyServiceId);
    }

    @Operation(summary = "List services")
    @GetMapping(path = "/services", produces = MediaType.APPLICATION_JSON_VALUE)
    public PagedModel<RelyingPartyServiceResponse> searchRelyingPartyServices(
        @Parameter(description = "Owning RelyingParty identifier")
        @RequestParam("partyId") UUID relyingPartyId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "25") int pageSize,
        @RequestParam(required = false) String sort
    ) {
        return new PagedModel<>(v2ApiService.searchRelyingPartyServices(
            relyingPartyId, page, pageSize, sort));
    }

    @Operation(
        summary = "Replace editable service properties",
        description = "Replaces editable properties only; omitted optional properties are cleared and relations preserved."
    )
    @Audit(auditId = AUDIT_SERVICE_UPDATED)
    @PutMapping(path = "/services/{id}", consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public RelyingPartyServiceResponse updateRelyingPartyService(
        @PathVariable("id") UUID relyingPartyServiceId,
        @Valid @RequestBody RelyingPartyServiceRequest request
    ) {
        return v2ApiService.updateRelyingPartyService(relyingPartyServiceId, request);
    }

    @Operation(summary = "Create a RelyingPartyInstance")
    @Audit(auditId = AUDIT_RELYING_PARTY_INSTANCE_CREATED)
    @PostMapping(path = "/services/{serviceId}/instances", consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RelyingPartyInstanceResponse> createRelyingPartyInstance(
        @Parameter(description = "Parent RelyingPartyService identifier")
        @PathVariable("serviceId") UUID relyingPartyServiceId,
        @Valid @RequestBody RelyingPartyInstanceRequest request
    ) {
        return ResponseEntity.ok(v2ApiService.createRelyingPartyInstance(relyingPartyServiceId, request));
    }

    @Operation(summary = "Get a RelyingPartyInstance")
    @GetMapping(path = "/instances/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public RelyingPartyInstanceResponse getRelyingPartyInstance(
        @PathVariable("id") UUID relyingPartyInstanceId
    ) {
        return v2ApiService.getRelyingPartyInstance(relyingPartyInstanceId);
    }

    @Operation(
        summary = "List registration summaries for an organization",
        description = "Returns the organization's registrations with instance IDs, service names, roles, and creation and update timestamps."
    )
    @GetMapping(path = "/registrations", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<RegistrationSummaryResponse> getRegistrationsByOrgno(@RequestParam @NotBlank @Orgnr String orgno) {
        return v2ApiService.getRegistrationsByOrgno(orgno);
    }

    @Operation(summary = "Search RelyingPartyInstances")
    @GetMapping(path = "/instances", produces = MediaType.APPLICATION_JSON_VALUE)
    public PagedModel<RelyingPartyInstanceResponse> searchRelyingPartyInstances(
        @Parameter(description = "Optional RelyingPartyService identifier")
        @RequestParam(value = "serviceId", required = false) UUID relyingPartyServiceId,
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
            relyingPartyServiceId, orgno, searchTerm, includeInactive, requiredEntitlements, page, pageSize, sort));
    }

    @Operation(
        summary = "Replace editable RelyingPartyInstance properties",
        description = "Replaces entitlements and attestations only; parent, status and certificates are preserved."
    )
    @Audit(auditId = AUDIT_RELYING_PARTY_INSTANCE_UPDATED)
    @PutMapping(path = "/instances/{id}", consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public RelyingPartyInstanceResponse updateRelyingPartyInstance(
        @PathVariable("id") UUID relyingPartyInstanceId,
        @Valid @RequestBody RelyingPartyInstanceRequest request
    ) {
        return v2ApiService.updateRelyingPartyInstance(relyingPartyInstanceId, request);
    }

    @Operation(summary = "List IntendedUse snapshots")
    @GetMapping(path = "/services/{serviceId}/intended-uses", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<IntendedUseResponse> getIntendedUses(
        @Parameter(description = "Owning RelyingPartyService identifier")
        @PathVariable("serviceId") UUID relyingPartyServiceId
    ) {
        return v2ApiService.getIntendedUses(relyingPartyServiceId);
    }

    @Operation(summary = "Create an IntendedUse snapshot")
    @Audit(auditId = AUDIT_INTENDED_USE_CREATED)
    @PostMapping(path = "/services/{serviceId}/intended-uses", consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<IntendedUseResponse> createIntendedUse(
        @Parameter(description = "Owning RelyingPartyService identifier")
        @PathVariable("serviceId") UUID relyingPartyServiceId,
        @Valid @RequestBody CreateIntendedUseRequest request
    ) {
        return ResponseEntity.ok(v2ApiService.createIntendedUse(relyingPartyServiceId, request));
    }

    @Operation(summary = "Replace an IntendedUse snapshot")
    @Audit(auditId = AUDIT_INTENDED_USE_REPLACED)
    @PostMapping(path = "/services/{serviceId}/intended-uses/{id}/replace",
        consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<IntendedUseReplacementResponse> replaceIntendedUse(
        @Parameter(description = "Owning RelyingPartyService identifier")
        @PathVariable("serviceId") UUID relyingPartyServiceId,
        @Parameter(description = "IntendedUse identifier to replace")
        @PathVariable UUID id,
        @Valid @RequestBody CreateIntendedUseRequest request
    ) {
        return ResponseEntity.ok(v2ApiService.replaceIntendedUse(relyingPartyServiceId, id, request));
    }

    @Operation(summary = "Revoke an IntendedUse snapshot")
    @Audit(auditId = AUDIT_INTENDED_USE_REVOKED)
    @PatchMapping(path = "/services/{serviceId}/intended-uses/{id}/revoke",
        produces = MediaType.APPLICATION_JSON_VALUE)
    public IntendedUseResponse revokeIntendedUse(
        @Parameter(description = "Owning RelyingPartyService identifier")
        @PathVariable("serviceId") UUID relyingPartyServiceId,
        @Parameter(description = "IntendedUse identifier to revoke")
        @PathVariable UUID id
    ) {
        return v2ApiService.revokeIntendedUse(relyingPartyServiceId, id);
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
    public CertificatesResponse getAccessCertificates(@PathVariable("instanceId") UUID relyingPartyInstanceId) {
        return v2ApiService.getAccessCertificates(relyingPartyInstanceId);
    }

    @Operation(summary = "List issuer certificates")
    @GetMapping(path = "/instances/{instanceId}/certificates/issuer",
        produces = MediaType.APPLICATION_JSON_VALUE)
    public CertificatesResponse getIssuerCertificates(@PathVariable("instanceId") UUID relyingPartyInstanceId) {
        return v2ApiService.getIssuerCertificates(relyingPartyInstanceId);
    }

    @Operation(summary = "Get an access certificate")
    @GetMapping(path = "/instances/{instanceId}/certificates/access/{certificateId}",
        produces = MediaType.APPLICATION_JSON_VALUE)
    public CertificateResponse getAccessCertificate(
        @PathVariable("instanceId") UUID relyingPartyInstanceId,
        @PathVariable UUID certificateId
    ) {
        return v2ApiService.getAccessCertificate(relyingPartyInstanceId, certificateId);
    }

    @Operation(summary = "Get an issuer certificate")
    @GetMapping(path = "/instances/{instanceId}/certificates/issuer/{certificateId}",
        produces = MediaType.APPLICATION_JSON_VALUE)
    public CertificateResponse getIssuerCertificate(
        @PathVariable("instanceId") UUID relyingPartyInstanceId,
        @PathVariable UUID certificateId
    ) {
        return v2ApiService.getIssuerCertificate(relyingPartyInstanceId, certificateId);
    }

    @Operation(summary = "Request an access certificate")
    @Audit(auditId = AUDIT_ACCESS_CERTIFICATE_CREATED)
    @PostMapping(path = "/instances/{instanceId}/certificates/access",
        consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public CertificateResponse requestAccessCertificate(
        @PathVariable("instanceId") UUID relyingPartyInstanceId,
        @Valid @RequestBody AccessCertificateRequest request
    ) {
        return v2ApiService.requestAccessCertificate(relyingPartyInstanceId, request);
    }

    @Operation(summary = "Request an issuer certificate")
    @Audit(auditId = AUDIT_ISSUER_CERTIFICATE_CREATED)
    @PostMapping(path = "/instances/{instanceId}/certificates/issuer",
        consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public CertificateResponse requestIssuerCertificate(
        @PathVariable("instanceId") UUID relyingPartyInstanceId,
        @Valid @RequestBody IssuerCertificateRequest request
    ) {
        return v2ApiService.requestIssuerCertificate(relyingPartyInstanceId, request);
    }

    @Operation(summary = "Revoke an access certificate")
    @Audit(auditId = AUDIT_ACCESS_CERTIFICATE_REVOKED)
    @PatchMapping(path = "/instances/{instanceId}/certificates/access/{certificateId}/revoke")
    public ResponseEntity<Void> revokeAccessCertificate(
        @PathVariable("instanceId") UUID relyingPartyInstanceId,
        @PathVariable UUID certificateId
    ) {
        v2ApiService.revokeAccessCertificate(relyingPartyInstanceId, certificateId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Revoke an issuer certificate")
    @Audit(auditId = AUDIT_ISSUER_CERTIFICATE_REVOKED)
    @PatchMapping(path = "/instances/{instanceId}/certificates/issuer/{certificateId}/revoke")
    public ResponseEntity<Void> revokeIssuerCertificate(
        @PathVariable("instanceId") UUID relyingPartyInstanceId,
        @PathVariable UUID certificateId
    ) {
        v2ApiService.revokeIssuerCertificate(relyingPartyInstanceId, certificateId);
        return ResponseEntity.noContent().build();
    }
}
