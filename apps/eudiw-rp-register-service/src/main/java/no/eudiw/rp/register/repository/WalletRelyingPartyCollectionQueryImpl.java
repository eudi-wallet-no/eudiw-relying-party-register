package no.eudiw.rp.register.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import no.eudiw.rp.register.domain.LegalPerson;
import no.eudiw.rp.register.domain.WalletRelyingParty;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
class WalletRelyingPartyCollectionQueryImpl implements WalletRelyingPartyCollectionQuery {
    private static final String IDENTIFIER_PARAMETER = "identifier";
    private static final String LEGAL_NAME_PARAMETER = "legalName";

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
            .where(buildPredicates(builder, party, criteria, cursorCreatedMs, cursorId))
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
    }
}
