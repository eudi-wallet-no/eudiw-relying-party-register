package no.eudiw.rp.register.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import no.eudiw.rp.register.domain.IntendedUse;
import no.eudiw.rp.register.domain.LegalPerson;
import no.eudiw.rp.register.domain.WalletRelyingParty;
import no.eudiw.rp.register.domain.WalletRelyingPartyService;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyEntitlement;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyInstance;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
class WalletRelyingPartyCollectionQueryImpl implements WalletRelyingPartyCollectionQuery {
    private static final String IDENTIFIER_PARAMETER = "identifier";
    private static final String LEGAL_NAME_PARAMETER = "legalName";
    private static final String SERVICE_IDENTIFIER_PARAMETER = "serviceIdentifier";
    private static final String TRADE_NAME_PARAMETER = "tradeName";
    private static final String POLICY_PARAMETER = "policy";
    private static final String ENTITLEMENT_PARAMETER = "entitlement";
    private static final String PROVIDED_ATTESTATION_PARAMETER = "providedAttestation";
    private static final String USES_INTERMEDIARY_PARAMETER = "usesIntermediary";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<WalletRelyingParty> findActivePage(
        WrpSearchCriteria criteria,
        Long cursorCreatedMs,
        UUID cursorId,
        int pageSize
    ) {
        if ((cursorCreatedMs == null) != (cursorId == null)) {
            throw new IllegalArgumentException("Both cursor values must be provided together");
        }

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<WalletRelyingParty> query = builder.createQuery(WalletRelyingParty.class);
        Root<WalletRelyingParty> party = query.from(WalletRelyingParty.class);
        party.fetch("legalPerson", JoinType.LEFT);

        query.select(party)
            .where(buildPredicates(builder, query, party, criteria, cursorCreatedMs, cursorId))
            .orderBy(
                builder.asc(party.get("createdMs")),
                builder.asc(party.get("id"))
            );

        TypedQuery<WalletRelyingParty> typedQuery = entityManager.createQuery(query).setMaxResults(pageSize);
        bindFilterParameters(typedQuery, criteria);
        return typedQuery.getResultList();
    }

    private static Predicate[] buildPredicates(
        CriteriaBuilder builder,
        CriteriaQuery<?> query,
        Root<WalletRelyingParty> party,
        WrpSearchCriteria criteria,
        Long cursorCreatedMs,
        UUID cursorId
    ) {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(builder.isTrue(party.get("active")));
        if (criteria.identifier() != null) {
            predicates.add(identifierPredicate(builder, party));
        }
        if (criteria.legalName() != null) {
            predicates.add(legalNamePredicate(builder, party));
        }
        if (criteria.serviceIdentifier() != null) {
            predicates.add(serviceIdentifierPredicate(builder, query, party));
        }
        if (criteria.tradeName() != null) {
            predicates.add(tradeNamePredicate(builder, query, party));
        }
        if (criteria.policy() != null) {
            predicates.add(policyPredicate(builder, query, party));
        }
        if (criteria.entitlement() != null) {
            predicates.add(entitlementPredicate(builder, query, party));
        }
        if (criteria.providedAttestation() != null) {
            predicates.add(providedAttestationPredicate(builder, query, party));
        }
        if (criteria.usesIntermediary() != null) {
            predicates.add(usesIntermediaryPredicate(builder, query, party));
        }
        if (cursorCreatedMs != null) {
            predicates.add(cursorPredicate(builder, party, cursorCreatedMs, cursorId));
        }
        return predicates.toArray(Predicate[]::new);
    }

    private static Predicate identifierPredicate(
        CriteriaBuilder builder,
        Root<WalletRelyingParty> party
    ) {
        ParameterExpression<String> identifier = builder.parameter(String.class, IDENTIFIER_PARAMETER);
        var identifierObject = builder.function(
            "JSON_OBJECT", String.class, builder.literal("identifier"), identifier);
        var containsIdentifier = builder.function(
            "JSON_CONTAINS",
            Integer.class,
            party.get("identifier"),
            identifierObject,
            builder.literal("$")
        );
        return builder.or(
            builder.equal(party.get("orgno"), identifier),
            builder.equal(containsIdentifier, 1)
        );
    }

    private static Predicate legalNamePredicate(
        CriteriaBuilder builder,
        Root<WalletRelyingParty> party
    ) {
        Join<WalletRelyingParty, LegalPerson> legalPerson = party.join("legalPerson", JoinType.INNER);
        ParameterExpression<String> legalName = builder.parameter(String.class, LEGAL_NAME_PARAMETER);
        var quotedLegalName = builder.function("JSON_QUOTE", String.class, legalName);
        var containsLegalName = builder.function(
            "JSON_CONTAINS",
            Integer.class,
            legalPerson.get("legalName"),
            quotedLegalName,
            builder.literal("$")
        );
        return builder.equal(containsLegalName, 1);
    }

    private static Predicate cursorPredicate(
        CriteriaBuilder builder,
        Root<WalletRelyingParty> party,
        long cursorCreatedMs,
        UUID cursorId
    ) {
        return builder.or(
            builder.greaterThan(party.get("createdMs"), cursorCreatedMs),
            builder.and(
                builder.equal(party.get("createdMs"), cursorCreatedMs),
                builder.greaterThan(party.<UUID>get("id"), cursorId)
            )
        );
    }

    private static Predicate serviceIdentifierPredicate(
        CriteriaBuilder builder,
        CriteriaQuery<?> query,
        Root<WalletRelyingParty> party
    ) {
        ParameterExpression<String> serviceIdentifier =
            builder.parameter(String.class, SERVICE_IDENTIFIER_PARAMETER);
        return activeServicePredicate(builder, query, party, "serviceIdentifier", serviceIdentifier);
    }

    private static Predicate tradeNamePredicate(
        CriteriaBuilder builder,
        CriteriaQuery<?> query,
        Root<WalletRelyingParty> party
    ) {
        ParameterExpression<String> tradeName = builder.parameter(String.class, TRADE_NAME_PARAMETER);
        return builder.or(
            builder.equal(party.get("tradeName"), tradeName),
            activeServicePredicate(builder, query, party, "serviceTradeName", tradeName)
        );
    }

    private static Predicate policyPredicate(
        CriteriaBuilder builder,
        CriteriaQuery<?> query,
        Root<WalletRelyingParty> party
    ) {
        ParameterExpression<String> policy = builder.parameter(String.class, POLICY_PARAMETER);
        var policyObject = builder.function(
            "JSON_OBJECT", String.class, builder.literal("policyURI"), policy);
        var containsWrpPolicy = builder.function(
            "JSON_CONTAINS",
            Integer.class,
            party.get("policy"),
            policyObject,
            builder.literal("$")
        );
        return builder.or(
            builder.equal(containsWrpPolicy, 1),
            intendedUsePolicyPredicate(builder, query, party, policyObject)
        );
    }

    private static Predicate intendedUsePolicyPredicate(
        CriteriaBuilder builder,
        CriteriaQuery<?> query,
        Root<WalletRelyingParty> party,
        Expression<String> policyObject
    ) {
        Subquery<Integer> matchingIntendedUse = query.subquery(Integer.class);
        Root<WalletRelyingPartyService> service = matchingIntendedUse.from(WalletRelyingPartyService.class);
        Join<WalletRelyingPartyService, IntendedUse> intendedUse =
            service.join("intendedUses", JoinType.INNER);
        var containsIntendedUsePolicy = builder.function(
            "JSON_CONTAINS",
            Integer.class,
            intendedUse.get("privacyPolicy"),
            policyObject,
            builder.literal("$")
        );
        matchingIntendedUse.select(builder.literal(1))
            .where(
                builder.equal(service.get("walletRelyingParty").get("id"), party.get("id")),
                builder.isTrue(service.get("active")),
                builder.equal(containsIntendedUsePolicy, 1)
            );
        return builder.exists(matchingIntendedUse);
    }

    private static Predicate entitlementPredicate(
        CriteriaBuilder builder,
        CriteriaQuery<?> query,
        Root<WalletRelyingParty> party
    ) {
        ParameterExpression<String> entitlement =
            builder.parameter(String.class, ENTITLEMENT_PARAMETER);
        return builder.or(
            serviceSubEntitlementPredicate(builder, query, party, entitlement),
            activeInstanceEntitlementPredicate(builder, query, party, entitlement)
        );
    }

    private static Predicate serviceSubEntitlementPredicate(
        CriteriaBuilder builder,
        CriteriaQuery<?> query,
        Root<WalletRelyingParty> party,
        ParameterExpression<String> entitlement
    ) {
        Subquery<Integer> matchingService = query.subquery(Integer.class);
        Root<WalletRelyingPartyService> service = matchingService.from(WalletRelyingPartyService.class);
        var containsSubEntitlement = builder.function(
            "JSON_CONTAINS",
            Integer.class,
            service.get("subEntitlements"),
            builder.function("JSON_QUOTE", String.class, entitlement),
            builder.literal("$")
        );
        matchingService.select(builder.literal(1))
            .where(
                builder.equal(service.get("walletRelyingParty").get("id"), party.get("id")),
                builder.isTrue(service.get("active")),
                builder.equal(containsSubEntitlement, 1)
            );
        return builder.exists(matchingService);
    }

    private static Predicate activeInstanceEntitlementPredicate(
        CriteriaBuilder builder,
        CriteriaQuery<?> query,
        Root<WalletRelyingParty> party,
        ParameterExpression<String> entitlementValue
    ) {
        Subquery<Integer> matchingInstanceEntitlement = query.subquery(Integer.class);
        Root<WalletRelyingPartyService> service =
            matchingInstanceEntitlement.from(WalletRelyingPartyService.class);
        Join<WalletRelyingPartyService, RelyingPartyInstance> instance =
            service.join("relyingPartyInstances", JoinType.INNER);
        Join<RelyingPartyInstance, RelyingPartyEntitlement> registeredEntitlement =
            instance.join("relyingPartyEntitlements", JoinType.INNER);
        var storedEntitlement = builder.function(
            "BINARY", String.class, registeredEntitlement.get("entitlement"));
        var requestedEntitlement = builder.function("BINARY", String.class, entitlementValue);
        matchingInstanceEntitlement.select(builder.literal(1))
            .where(
                builder.equal(service.get("walletRelyingParty").get("id"), party.get("id")),
                builder.isTrue(service.get("active")),
                builder.isTrue(instance.get("active")),
                builder.equal(storedEntitlement, requestedEntitlement)
            );
        return builder.exists(matchingInstanceEntitlement);
    }

    private static Predicate providedAttestationPredicate(
        CriteriaBuilder builder,
        CriteriaQuery<?> query,
        Root<WalletRelyingParty> party
    ) {
        ParameterExpression<String> providedAttestation =
            builder.parameter(String.class, PROVIDED_ATTESTATION_PARAMETER);
        var providedAttestationObject = builder.function(
            "JSON_OBJECT", String.class, builder.literal("type"), providedAttestation);
        Subquery<Integer> matchingService = query.subquery(Integer.class);
        Root<WalletRelyingPartyService> service = matchingService.from(WalletRelyingPartyService.class);
        var containsProvidedAttestation = builder.function(
            "JSON_CONTAINS",
            Integer.class,
            service.get("providesAttestations"),
            providedAttestationObject,
            builder.literal("$")
        );
        matchingService.select(builder.literal(1))
            .where(
                builder.equal(service.get("walletRelyingParty").get("id"), party.get("id")),
                builder.isTrue(service.get("active")),
                builder.equal(containsProvidedAttestation, 1)
            );
        return builder.exists(matchingService);
    }

    private static Predicate usesIntermediaryPredicate(
        CriteriaBuilder builder,
        CriteriaQuery<?> query,
        Root<WalletRelyingParty> party
    ) {
        ParameterExpression<String> usesIntermediary =
            builder.parameter(String.class, USES_INTERMEDIARY_PARAMETER);
        var intermediaryReference = builder.function(
            "JSON_OBJECT", String.class, builder.literal("serviceIdentifier"), usesIntermediary);
        Subquery<Integer> matchingService = query.subquery(Integer.class);
        Root<WalletRelyingPartyService> service = matchingService.from(WalletRelyingPartyService.class);
        var containsIntermediary = builder.function(
            "JSON_CONTAINS",
            Integer.class,
            service.get("usesIntermediaries"),
            intermediaryReference,
            builder.literal("$")
        );
        matchingService.select(builder.literal(1))
            .where(
                builder.equal(service.get("walletRelyingParty").get("id"), party.get("id")),
                builder.isTrue(service.get("active")),
                builder.equal(containsIntermediary, 1)
            );
        return builder.exists(matchingService);
    }

    private static Predicate activeServicePredicate(
        CriteriaBuilder builder,
        CriteriaQuery<?> query,
        Root<WalletRelyingParty> party,
        String serviceAttribute,
        ParameterExpression<String> value
    ) {
        Subquery<Integer> matchingService = query.subquery(Integer.class);
        Root<WalletRelyingParty> correlatedParty = matchingService.correlate(party);
        Join<WalletRelyingParty, WalletRelyingPartyService> service =
            correlatedParty.join("services", JoinType.INNER);
        matchingService.select(builder.literal(1))
            .where(
                builder.isTrue(service.get("active")),
                builder.equal(service.get(serviceAttribute), value)
            );
        return builder.exists(matchingService);
    }

    private static void bindFilterParameters(
        TypedQuery<WalletRelyingParty> query,
        WrpSearchCriteria criteria
    ) {
        if (criteria.identifier() != null) {
            query.setParameter(IDENTIFIER_PARAMETER, criteria.identifier());
        }
        if (criteria.legalName() != null) {
            query.setParameter(LEGAL_NAME_PARAMETER, criteria.legalName());
        }
        if (criteria.serviceIdentifier() != null) {
            query.setParameter(SERVICE_IDENTIFIER_PARAMETER, criteria.serviceIdentifier());
        }
        if (criteria.tradeName() != null) {
            query.setParameter(TRADE_NAME_PARAMETER, criteria.tradeName());
        }
        if (criteria.policy() != null) {
            query.setParameter(POLICY_PARAMETER, criteria.policy());
        }
        if (criteria.entitlement() != null) {
            query.setParameter(ENTITLEMENT_PARAMETER, criteria.entitlement());
        }
        if (criteria.providedAttestation() != null) {
            query.setParameter(PROVIDED_ATTESTATION_PARAMETER, criteria.providedAttestation());
        }
        if (criteria.usesIntermediary() != null) {
            query.setParameter(USES_INTERMEDIARY_PARAMETER, criteria.usesIntermediary());
        }
    }
}
