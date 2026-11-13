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
import jakarta.persistence.criteria.Subquery;
import no.eudiw.rp.register.domain.Credential;
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
    private static final String CREDENTIAL_FORMAT_PARAMETER = "credentialFormat";
    private static final String CLAIM_PATH_PARAMETER = "claimPath";
    private static final String CREDENTIAL_META_PARAMETER = "credentialMeta";
    private static final String POLICY_PARAMETER = "policy";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<IntendedUse> findAllMatchingCheckFilters(
        String identifier,
        String serviceIdentifier,
        String intendedUseIdentifier,
        String credentialFormat,
        String claimPath,
        String credentialMeta,
        String policy
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
        if (credentialFormat != null) {
            predicates.add(credentialFormatPredicate(builder, query, intendedUse));
        }
        if (claimPath != null) {
            predicates.add(claimPathPredicate(builder, query, intendedUse));
        }
        if (credentialMeta != null) {
            predicates.add(credentialMetaPredicate(builder, query, intendedUse));
        }
        if (policy != null) {
            predicates.add(policyPredicate(builder, intendedUse));
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
        if (credentialFormat != null) {
            typedQuery.setParameter(CREDENTIAL_FORMAT_PARAMETER, credentialFormat);
        }
        if (claimPath != null) {
            typedQuery.setParameter(CLAIM_PATH_PARAMETER, claimPath);
        }
        if (credentialMeta != null) {
            typedQuery.setParameter(CREDENTIAL_META_PARAMETER, credentialMeta);
        }
        if (policy != null) {
            typedQuery.setParameter(POLICY_PARAMETER, policy);
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

    private static Predicate credentialFormatPredicate(
        CriteriaBuilder builder,
        CriteriaQuery<?> query,
        Root<IntendedUse> intendedUse
    ) {
        ParameterExpression<String> credentialFormat =
            builder.parameter(String.class, CREDENTIAL_FORMAT_PARAMETER);
        Subquery<Integer> matchingCredential = query.subquery(Integer.class);
        Root<Credential> credential = matchingCredential.from(Credential.class);
        var storedFormat = builder.function("BINARY", String.class, credential.get("format"));
        var requestedFormat = builder.function("BINARY", String.class, credentialFormat);
        matchingCredential.select(builder.literal(1))
            .where(
                builder.equal(credential.get("intendedUse").get("id"), intendedUse.get("id")),
                builder.equal(storedFormat, requestedFormat)
            );
        return builder.exists(matchingCredential);
    }

    private static Predicate claimPathPredicate(
        CriteriaBuilder builder,
        CriteriaQuery<?> query,
        Root<IntendedUse> intendedUse
    ) {
        ParameterExpression<String> claimPath = builder.parameter(String.class, CLAIM_PATH_PARAMETER);
        var claimPathArray = builder.function("JSON_ARRAY", String.class, claimPath);
        var claim = builder.function("JSON_OBJECT", String.class, builder.literal("path"), claimPathArray);
        Subquery<Integer> matchingClaimPath = query.subquery(Integer.class);
        Root<Credential> credential = matchingClaimPath.from(Credential.class);
        var containsClaimPath = builder.function("JSON_CONTAINS", Integer.class, credential.get("claims"), claim);
        matchingClaimPath.select(builder.literal(1))
            .where(
                builder.equal(credential.get("intendedUse").get("id"), intendedUse.get("id")),
                builder.equal(containsClaimPath, 1)
            );
        return builder.exists(matchingClaimPath);
    }

    private static Predicate credentialMetaPredicate(
        CriteriaBuilder builder,
        CriteriaQuery<?> query,
        Root<IntendedUse> intendedUse
    ) {
        ParameterExpression<String> credentialMeta =
            builder.parameter(String.class, CREDENTIAL_META_PARAMETER);
        var vctValuesCandidate = builder.function(
            "JSON_OBJECT",
            String.class,
            builder.literal("vct_values"),
            builder.function("JSON_ARRAY", String.class, credentialMeta)
        );
        var doctypeCandidate = builder.function(
            "JSON_OBJECT", String.class, builder.literal("doctype"), credentialMeta);
        Subquery<Integer> matchingCredential = query.subquery(Integer.class);
        Root<Credential> credential = matchingCredential.from(Credential.class);
        var containsVctValue = builder.function(
            "JSON_CONTAINS", Integer.class, credential.get("meta"), vctValuesCandidate);
        var containsDoctype = builder.function(
            "JSON_CONTAINS", Integer.class, credential.get("meta"), doctypeCandidate);
        matchingCredential.select(builder.literal(1))
            .where(
                builder.equal(credential.get("intendedUse").get("id"), intendedUse.get("id")),
                builder.or(
                    builder.equal(containsVctValue, 1),
                    builder.equal(containsDoctype, 1)
                )
            );
        return builder.exists(matchingCredential);
    }

    private static Predicate policyPredicate(
        CriteriaBuilder builder,
        Root<IntendedUse> intendedUse
    ) {
        ParameterExpression<String> policy = builder.parameter(String.class, POLICY_PARAMETER);
        var policyObject = builder.function(
            "JSON_OBJECT", String.class, builder.literal("policyURI"), policy);
        var containsPolicy = builder.function(
            "JSON_CONTAINS",
            Integer.class,
            intendedUse.get("privacyPolicy"),
            policyObject,
            builder.literal("$")
        );
        return builder.equal(containsPolicy, 1);
    }
}
