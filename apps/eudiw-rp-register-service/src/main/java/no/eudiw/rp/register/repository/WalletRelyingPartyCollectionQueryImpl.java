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

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(builder.isTrue(party.get("active")));

        if (criteria.identifier() != null) {
            addIdentifierCriteria(builder, party, predicates);
        }

        if (criteria.legalName() != null) {
            addLegalnameCriteria(party, builder, predicates);
        }

        if (cursorCreatedMs != null) {
            predicates.add(builder.or(
                builder.greaterThan(party.get("createdMs"), cursorCreatedMs),
                builder.and(
                    builder.equal(party.get("createdMs"), cursorCreatedMs),
                    builder.greaterThan(party.<UUID>get("id"), cursorId)
                )
            ));
        }

        query.select(party)
            .where(predicates.toArray(Predicate[]::new))
            .orderBy(
                builder.asc(party.get("createdMs")),
                builder.asc(party.get("id"))
            );

        TypedQuery<WalletRelyingParty> typedQuery = entityManager.createQuery(query)
            .setMaxResults(pageSize);
        if (criteria.identifier() != null) {
            typedQuery.setParameter("identifier", criteria.identifier());
        }
        if (criteria.legalName() != null) {
            typedQuery.setParameter("legalName", criteria.legalName());
        }
        return typedQuery.getResultList();
    }

    private static void addLegalnameCriteria(
            Root<WalletRelyingParty> party,
            CriteriaBuilder builder,
            List<Predicate> predicates) {
        Join<WalletRelyingParty, LegalPerson> legalPerson = party.join("legalPerson", JoinType.INNER);
        ParameterExpression<String> legalName =
            builder.parameter(String.class, "legalName");
        var quotedLegalName = builder.function(
            "JSON_QUOTE",
            String.class,
            legalName
        );
        var containsLegalName = builder.function(
            "JSON_CONTAINS",
            Integer.class,
            legalPerson.get("legalName"),
            quotedLegalName,
            builder.literal("$")
        );
        predicates.add(builder.equal(containsLegalName, 1));
    }

    private static void addIdentifierCriteria(
            CriteriaBuilder builder,
            Root<WalletRelyingParty> party,
            List<Predicate> predicates
    ) {
        ParameterExpression<String> identifier = builder.parameter(String.class, "identifier");
        var identifierObject = builder.function(
            "JSON_OBJECT",
            String.class,
            builder.literal("identifier"),
            identifier
        );
        var containsIdentifier = builder.function(
            "JSON_CONTAINS",
            Integer.class,
            party.get("identifier"),
            identifierObject,
            builder.literal("$")
        );
        predicates.add(builder.or(
            builder.equal(party.get("orgno"), identifier),
            builder.equal(containsIdentifier, 1)
        ));
    }
}
