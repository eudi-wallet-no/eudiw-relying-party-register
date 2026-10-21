package no.eudiw.rp.register.api.v2;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import no.eudiw.rp.register.api.v2.resource.V2Resources;
import no.eudiw.rp.register.config.RegistryProperties;
import no.eudiw.rp.register.config.SupervisoryAuthorityProperties;
import no.eudiw.rp.register.domain.Credential;
import no.eudiw.rp.register.domain.Identifier;
import no.eudiw.rp.register.domain.IntendedUse;
import no.eudiw.rp.register.domain.IntermediaryReference;
import no.eudiw.rp.register.domain.Law;
import no.eudiw.rp.register.domain.LegalPerson;
import no.eudiw.rp.register.domain.MultiLangString;
import no.eudiw.rp.register.domain.Policy;
import no.eudiw.rp.register.domain.ProvidedAttestation;
import no.eudiw.rp.register.domain.WalletRelyingParty;
import no.eudiw.rp.register.domain.WalletRelyingPartyService;
import no.eudiw.rp.register.domain.certificates.AccessCertificate;
import no.eudiw.rp.register.domain.certificates.IssuerCertificate;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyEaa;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyEntitlement;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyInstance;
import no.eudiw.rp.register.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class V2ContractMapper {

    private final Validator validator;
    private final RegistryProperties registryProperties;
    private final SupervisoryAuthorityProperties supervisoryAuthorityProperties;

    public V2Resources.WalletRelyingPartyResource toWalletRelyingPartyResource(
        WalletRelyingParty walletRelyingParty
    ) {
        return new V2Resources.WalletRelyingPartyResource(
            walletRelyingParty.getId(),
            walletRelyingParty.getOrgno(),
            walletRelyingParty.getTradeName(),
            walletRelyingParty.isPSB(),
            walletRelyingParty.getCountry(),
            walletRelyingParty.getProviderType(),
            toLegalPersonResource(walletRelyingParty.getLegalPerson()),
            walletRelyingParty.getIdentifier().stream()
                .map(identifier -> new V2Resources.IdentifierResource(identifier.type(), identifier.identifier()))
                .toList(),
            walletRelyingParty.getPolicy().stream()
                .map(policy -> new V2Resources.PolicyResource(policy.type(), policy.policyURI()))
                .toList(),
            List.copyOf(walletRelyingParty.getX5c()),
            walletRelyingParty.getPostalAddress(),
            walletRelyingParty.getEmail(),
            walletRelyingParty.getPhone(),
            walletRelyingParty.getInfoURI(),
            walletRelyingParty.isActive(),
            walletRelyingParty.getCreatedMs(),
            walletRelyingParty.getLastUpdatedMs(),
            walletRelyingParty.getServices().stream().map(WalletRelyingPartyService::getId).toList(),
            toSupervisoryAuthorityResource(),
            registryProperties.registryUri()
        );
    }

    public V2Resources.WalletRelyingPartyServiceResource toWalletRelyingPartyServiceResource(
        WalletRelyingPartyService walletRelyingPartyService
    ) {
        return new V2Resources.WalletRelyingPartyServiceResource(
            walletRelyingPartyService.getId(),
            walletRelyingPartyService.getWalletRelyingParty().getId(),
            walletRelyingPartyService.getServiceTradeName(),
            walletRelyingPartyService.getServiceIdentifier(),
            walletRelyingPartyService.getSupportUri(),
            walletRelyingPartyService.getEmail(),
            walletRelyingPartyService.getPhone(),
            toNestedMultiLangStringResource(walletRelyingPartyService.getSrvDescription()),
            walletRelyingPartyService.getEntitlements(),
            walletRelyingPartyService.getSubEntitlements(),
            walletRelyingPartyService.getProvidesAttestations().stream()
                .map(item -> new V2Resources.ProvidedAttestationResource(item.format(), item.type()))
                .toList(),
            walletRelyingPartyService.getUsesIntermediaries().stream()
                .map(item -> new V2Resources.IntermediaryReferenceResource(
                    item.serviceIdentifier(), item.serviceTradeName(), item.registryURI()))
                .toList(),
            walletRelyingPartyService.isIntermediary(),
            walletRelyingPartyService.getServedWRPServices(),
            walletRelyingPartyService.isActive(),
            walletRelyingPartyService.getCreatedMs(),
            walletRelyingPartyService.getLastUpdatedMs(),
            walletRelyingPartyService.getRelyingPartyInstances().stream().map(RelyingPartyInstance::getId).toList(),
            walletRelyingPartyService.getIntendedUses().stream().map(IntendedUse::getId).toList()
        );
    }

    public V2Resources.RelyingPartyInstanceResource toRelyingPartyInstanceResource(
        RelyingPartyInstance relyingPartyInstance
    ) {
        return new V2Resources.RelyingPartyInstanceResource(
            relyingPartyInstance.getId(),
            relyingPartyInstance.getWalletRelyingPartyService().getId(),
            relyingPartyInstance.getRelyingPartyEntitlements().stream()
                .map(item -> new V2Resources.RelyingPartyEntitlementResource(
                    item.getEntitlement(), item.getCredentialIssuerUrl()))
                .toList(),
            relyingPartyInstance.getRelyingPartyEaas().stream()
                .map(item -> new V2Resources.RelyingPartyEaaResource(item.getNamespace(), item.getIntent()))
                .toList(),
            relyingPartyInstance.isActive(),
            relyingPartyInstance.getCreatedMs(),
            relyingPartyInstance.getLastUpdatedMs(),
            relyingPartyInstance.getAccessCertificates().stream().map(certificate -> certificate.getId()).toList(),
            relyingPartyInstance.getIssuerCertificates().stream().map(certificate -> certificate.getId()).toList()
        );
    }

    public V2Resources.IntendedUseResource toIntendedUseResource(IntendedUse intendedUse) {
        return new V2Resources.IntendedUseResource(
            intendedUse.getId(),
            intendedUse.getWalletRelyingPartyService().getId(),
            intendedUse.getIntendedUseIdentifier(),
            intendedUse.getCreatedAt(),
            intendedUse.getRevokedAt(),
            toMultiLangStringResource(intendedUse.getPurpose()),
            intendedUse.getPrivacyPolicy().stream()
                .map(policy -> new V2Resources.PolicyResource(policy.type(), policy.policyURI()))
                .toList(),
            intendedUse.getCredentials().stream()
                .map(credential -> new V2Resources.CredentialResource(
                    credential.getFormat(), credential.getMeta(), credential.getClaims()))
                .toList()
        );
    }

    public V2Resources.CertificateResource toCertificateResource(AccessCertificate certificate) {
        return new V2Resources.CertificateResource(
            certificate.getCertificate(), null, certificate.getId(), certificate.getRevocationStatus());
    }

    public V2Resources.CertificateResource toCertificateResource(IssuerCertificate certificate) {
        return new V2Resources.CertificateResource(
            certificate.getCertificate(),
            certificate.getEntitlement().getEntitlement(),
            certificate.getId(),
            certificate.getRevocationStatus()
        );
    }

    public WalletRelyingPartyService toWalletRelyingPartyService(
        V2Resources.WalletRelyingPartyServiceWriteResource resource,
        WalletRelyingParty walletRelyingParty,
        List<RelyingPartyInstance> relyingPartyInstances
    ) {
        validate(resource);
        WalletRelyingPartyService walletRelyingPartyService = new WalletRelyingPartyService(
            resource.serviceTradeName(),
            walletRelyingParty,
            relyingPartyInstances,
            resource.isIntermediary(),
            listOrEmpty(resource.usesIntermediaries()).stream()
                .map(item -> new IntermediaryReference(
                    item.serviceIdentifier(), item.serviceTradeName(), item.registryUri()))
                .toList(),
            listOrEmpty(resource.servedWRPServices())
        );
        walletRelyingPartyService.setServiceIdentifier(resource.serviceIdentifier());
        walletRelyingPartyService.setSupportUri(resource.supportUri());
        walletRelyingPartyService.setEmail(resource.email());
        walletRelyingPartyService.setPhone(resource.phone());
        walletRelyingPartyService.setSrvDescription(toNestedDomainMultiLangStrings(resource.srvDescription()));
        walletRelyingPartyService.setSubEntitlements(listOrEmpty(resource.subEntitlements()));
        walletRelyingPartyService.setProvidesAttestations(listOrEmpty(resource.providesAttestations()).stream()
            .map(item -> new ProvidedAttestation(item.format(), item.type()))
            .toList());
        return walletRelyingPartyService;
    }

    public List<RelyingPartyEntitlement> toDomainRelyingPartyEntitlements(
        V2Resources.RelyingPartyInstanceWriteResource resource
    ) {
        validate(resource);
        return toDomainRelyingPartyEntitlements(resource.relyingPartyEntitlements());
    }

    public List<RelyingPartyEntitlement> toDomainRelyingPartyEntitlements(
        List<V2Resources.RelyingPartyEntitlementResource> resources
    ) {
        if (resources == null) {
            throw new BadRequestException("Entitlements should be empty if none exists");
        }
        if (resources.stream().anyMatch(Objects::isNull)) {
            throw new BadRequestException("RelyingPartyInstance entitlements must not contain null");
        }
        long distinctEntitlements = resources.stream()
            .map(V2Resources.RelyingPartyEntitlementResource::entitlement)
            .distinct()
            .count();
        if (distinctEntitlements != resources.size()) {
            throw new BadRequestException("RelyingPartyInstance entitlements must be unique");
        }
        return resources.stream()
            .map(item -> new RelyingPartyEntitlement(item.entitlement(), item.credentialIssuerUrl()))
            .toList();
    }

    public List<RelyingPartyEaa> toDomainRelyingPartyEaas(
        List<V2Resources.RelyingPartyEaaResource> resources
    ) {
        return resources.stream().map(item -> new RelyingPartyEaa(item.namespace(), item.intent())).toList();
    }

    public IntendedUse toDomainIntendedUse(V2Resources.CreateIntendedUseResource resource) {
        return toDomainIntendedUse(resource, LocalDate.now(ZoneOffset.UTC).toString());
    }

    public IntendedUse toDomainIntendedUse(
        V2Resources.CreateIntendedUseResource resource,
        String createdAt
    ) {
        validate(resource);
        IntendedUse intendedUse = new IntendedUse(
            UUID.randomUUID().toString(),
            createdAt,
            null,
            toDomainMultiLangStrings(resource.purpose()),
            resource.privacyPolicy().stream()
                .map(item -> new Policy(item.type(), item.policyURI()))
                .toList()
        );
        intendedUse.setCredentials(resource.credentials().stream()
            .map(item -> new Credential(item.format(), item.meta(), item.claims()))
            .toList());
        return intendedUse;
    }

    public void updateWalletRelyingParty(
        WalletRelyingParty walletRelyingParty,
        V2Resources.WalletRelyingPartyWriteResource resource
    ) {
        validate(resource);
        walletRelyingParty.setTradeName(resource.tradeName());
        walletRelyingParty.setPSB(resource.isPSB());
        walletRelyingParty.setCountry(resource.country());
        if (resource.legalPerson() == null) {
            walletRelyingParty.setLegalPerson(null);
        } else if (walletRelyingParty.getLegalPerson() == null) {
            walletRelyingParty.setLegalPerson(toDomainLegalPerson(resource.legalPerson()));
        } else {
            walletRelyingParty.getLegalPerson().setLegalName(resource.legalPerson().legalName());
            walletRelyingParty.getLegalPerson().setEstablishedByLaw(listOrEmpty(
                resource.legalPerson().establishedByLaw()).stream()
                .map(item -> new Law(item.legislativeIdentifier(), item.legalBasis()))
                .toList());
        }
        walletRelyingParty.setIdentifier(resource.identifier().stream()
            .map(item -> new Identifier(item.type(), item.identifier()))
            .toList());
        walletRelyingParty.setPolicy(resource.policy().stream()
            .map(item -> new Policy(item.type(), item.policyURI()))
            .toList());
        walletRelyingParty.setX5c(listOrEmpty(resource.x5c()));
        walletRelyingParty.setPostalAddress(listOrEmpty(resource.postalAddress()));
        walletRelyingParty.setEmail(listOrEmpty(resource.email()));
        walletRelyingParty.setPhone(listOrEmpty(resource.phone()));
        walletRelyingParty.setInfoURI(listOrEmpty(resource.infoURI()));
    }

    public void updateWalletRelyingPartyService(
        WalletRelyingPartyService walletRelyingPartyService,
        V2Resources.WalletRelyingPartyServiceWriteResource resource
    ) {
        validate(resource);
        walletRelyingPartyService.setIntermediaryConfiguration(
            resource.isIntermediary(),
            listOrEmpty(resource.usesIntermediaries()).stream()
                .map(item -> new IntermediaryReference(
                    item.serviceIdentifier(), item.serviceTradeName(), item.registryUri()))
                .toList(),
            listOrEmpty(resource.servedWRPServices())
        );
        walletRelyingPartyService.setServiceTradeName(resource.serviceTradeName());
        walletRelyingPartyService.setServiceIdentifier(resource.serviceIdentifier());
        walletRelyingPartyService.setSupportUri(resource.supportUri());
        walletRelyingPartyService.setEmail(resource.email());
        walletRelyingPartyService.setPhone(resource.phone());
        walletRelyingPartyService.setSrvDescription(toNestedDomainMultiLangStrings(resource.srvDescription()));
        walletRelyingPartyService.setSubEntitlements(listOrEmpty(resource.subEntitlements()));
        walletRelyingPartyService.setProvidesAttestations(listOrEmpty(resource.providesAttestations()).stream()
            .map(item -> new ProvidedAttestation(item.format(), item.type()))
            .toList());
    }

    public void updateRelyingPartyInstance(
        RelyingPartyInstance relyingPartyInstance,
        V2Resources.RelyingPartyInstanceWriteResource resource,
        List<RelyingPartyEntitlement> relyingPartyEntitlements
    ) {
        validate(resource);
        for (RelyingPartyEntitlement existing : relyingPartyInstance.getRelyingPartyEntitlements()) {
            if (existing.getIssuerCertificates().isEmpty()) {
                continue;
            }
            boolean unchanged = resource.relyingPartyEntitlements().stream()
                .anyMatch(item -> item.entitlement().equals(existing.getEntitlement())
                    && Objects.equals(item.credentialIssuerUrl(), existing.getCredentialIssuerUrl()));
            if (!unchanged) {
                throw new BadRequestException(
                    "An entitlement with issuer certificates cannot be removed or changed");
            }
        }
        relyingPartyInstance.setRelyingPartyEntitlements(relyingPartyEntitlements);
        relyingPartyInstance.setRelyingPartyEaas(toDomainRelyingPartyEaas(resource.relyingPartyEaas()));
    }

    private LegalPerson toDomainLegalPerson(V2Resources.LegalPersonResource resource) {
        return new LegalPerson(
            resource.legalName(),
            listOrEmpty(resource.establishedByLaw()).stream()
                .map(item -> new Law(item.legislativeIdentifier(), item.legalBasis()))
                .toList()
        );
    }

    private V2Resources.LegalPersonResource toLegalPersonResource(LegalPerson legalPerson) {
        if (legalPerson == null) {
            return null;
        }
        return new V2Resources.LegalPersonResource(
            legalPerson.getLegalName(),
            legalPerson.getEstablishedByLaw().stream()
                .map(item -> new V2Resources.LawResource(item.legislativeIdentifier(), item.legalBasis()))
                .toList()
        );
    }

    private V2Resources.SupervisoryAuthorityResource toSupervisoryAuthorityResource() {
        return new V2Resources.SupervisoryAuthorityResource(
            supervisoryAuthorityProperties.name(),
            supervisoryAuthorityProperties.country(),
            singletonIfPresent(supervisoryAuthorityProperties.phone()),
            singletonIfPresent(supervisoryAuthorityProperties.email()),
            singletonIfPresent(supervisoryAuthorityProperties.formUri())
        );
    }

    private static List<String> singletonIfPresent(String value) {
        return value == null || value.isBlank() ? List.of() : List.of(value);
    }

    private static <T> List<T> listOrEmpty(List<T> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }

    private List<List<V2Resources.MultiLangStringResource>> toNestedMultiLangStringResource(
        List<List<MultiLangString>> values
    ) {
        if (values == null) {
            return null;
        }
        return values.stream().map(this::toMultiLangStringResource).toList();
    }

    private List<V2Resources.MultiLangStringResource> toMultiLangStringResource(List<MultiLangString> values) {
        return values.stream()
            .map(item -> new V2Resources.MultiLangStringResource(item.lang(), item.content()))
            .toList();
    }

    private List<List<MultiLangString>> toNestedDomainMultiLangStrings(
        List<List<V2Resources.MultiLangStringResource>> values
    ) {
        if (values == null) {
            return null;
        }
        return values.stream().map(this::toDomainMultiLangStrings).toList();
    }

    private List<MultiLangString> toDomainMultiLangStrings(List<V2Resources.MultiLangStringResource> values) {
        return values.stream().map(item -> new MultiLangString(item.lang(), item.content())).toList();
    }

    private <T> void validate(T resource) {
        Set<ConstraintViolation<T>> violations = validator.validate(resource);
        if (!violations.isEmpty()) {
            throw validationException(violations);
        }
    }

    private static BadRequestException validationException(Set<? extends ConstraintViolation<?>> violations) {
        String message = violations.stream()
            .map(ConstraintViolation::getMessage)
            .sorted()
            .reduce((left, right) -> left + ", " + right)
            .orElse("Invalid request");
        return new BadRequestException(message);
    }
}
