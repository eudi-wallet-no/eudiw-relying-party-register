package no.eudiw.rp.register.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import no.eudiw.rp.register.domain.IntendedUse;
import no.eudiw.rp.register.domain.WalletRelyingParty;
import no.eudiw.rp.register.domain.WalletRelyingPartyService;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
class IntendedUseCheckQueryImpl implements IntendedUseCheckQuery {
    private static final String IDENTIFIER_PARAMETER = "identifier";
    private static final String SERVICE_IDENTIFIER_PARAMETER = "serviceIdentifier";
    private static final String INTENDED_USE_IDENTIFIER_PARAMETER = "intendedUseIdentifier";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<IntendedUse> findAllMatchingCheckFilters(
        String identifier,
        String serviceIdentifier,
        String intendedUseIdentifier
    ) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<IntendedUse> query = builder.createQuery(IntendedUse.class);
        Root<IntendedUse> intendedUse = query.from(IntendedUse.class);
        Join<IntendedUse, WalletRelyingPartyService> service =
            intendedUse.join("walletRelyingPartyService", JoinType.INNER);
        Join<WalletRelyingPartyService, WalletRelyingParty> party =
            service.join("walletRelyingParty", JoinType.INNER);

        List<Predicate> predicates = new ArrayList<>();
        if (identifier != null) {
            predicates.add(identifierPredicate(builder, party));
        }
        if (serviceIdentifier != null) {
            predicates.add(serviceIdentifierPredicate(builder, service));
        }
        if (intendedUseIdentifier != null) {
            predicates.add(intendedUseIdentifierPredicate(builder, intendedUse));
        }

        query.select(intendedUse).where(predicates.toArray(Predicate[]::new));
        TypedQuery<IntendedUse> typedQuery = entityManager.createQuery(query);
        if (identifier != null) {
            typedQuery.setParameter(IDENTIFIER_PARAMETER, identifier);
        }
        if (serviceIdentifier != null) {
            typedQuery.setParameter(SERVICE_IDENTIFIER_PARAMETER, serviceIdentifier);
        }
        if (intendedUseIdentifier != null) {
            typedQuery.setParameter(INTENDED_USE_IDENTIFIER_PARAMETER, intendedUseIdentifier);
        }
        return typedQuery.getResultList();
    }

    private static Predicate identifierPredicate(
        CriteriaBuilder builder,
        From<?, WalletRelyingParty> party
    ) {
        ParameterExpression<String> identifier =
            builder.parameter(String.class, IDENTIFIER_PARAMETER);
        var storedOrgno = builder.function("BINARY", String.class, party.get("orgno"));
        var requestedIdentifier = builder.function("BINARY", String.class, identifier);
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
            builder.equal(storedOrgno, requestedIdentifier),
            builder.equal(containsIdentifier, 1)
        );
    }

    private static Predicate serviceIdentifierPredicate(
        CriteriaBuilder builder,
        Join<IntendedUse, WalletRelyingPartyService> service
    ) {
        ParameterExpression<String> serviceIdentifier =
            builder.parameter(String.class, SERVICE_IDENTIFIER_PARAMETER);
        var storedIdentifier = builder.function("BINARY", String.class, service.get("serviceIdentifier"));
        var requestedIdentifier = builder.function("BINARY", String.class, serviceIdentifier);
        return builder.equal(storedIdentifier, requestedIdentifier);
    }

    private static Predicate intendedUseIdentifierPredicate(
        CriteriaBuilder builder,
        Root<IntendedUse> intendedUse
    ) {
        ParameterExpression<String> intendedUseIdentifier =
            builder.parameter(String.class, INTENDED_USE_IDENTIFIER_PARAMETER);
        var storedIdentifier =
            builder.function("BINARY", String.class, intendedUse.get("intendedUseIdentifier"));
        var requestedIdentifier = builder.function("BINARY", String.class, intendedUseIdentifier);
        return builder.equal(storedIdentifier, requestedIdentifier);
    }
}
