package it.polimi.se.bbp.specification;

import it.polimi.se.bbp.dto.request.BikePathSearchRequest;
import it.polimi.se.bbp.entity.BikePath;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * JPA Specification for building dynamic bike path search queries.
 * Implements Specification pattern for type-safe queries using Criteria API.
 * All filters combined with AND logic.
 */
public record BikePathSpecification(

        /* ID of user who created bike paths */
        Long userId,

        /* Search request with filter criteria */
        BikePathSearchRequest searchRequest

) implements Specification<BikePath> {

    /**
     * Constructs JPA Predicate for search query.
     * Combines all non-null filters with AND logic.
     * Always filters by user ID to ensure users only see their own bike paths.
     * @param root root entity (BikePath)
     * @param query CriteriaQuery being constructed
     * @param criteriaBuilder CriteriaBuilder for creating predicates
     * @return combined Predicate representing all filters
     */
    @Override
    public Predicate toPredicate(Root<BikePath> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
        List<Predicate> predicates = new ArrayList<>();
        // Always filter by user ID
        predicates.add(criteriaBuilder.equal(root.get("createdBy").get("id"), userId));
        addOriginFilter(predicates, root, criteriaBuilder);
        addDestinationFilter(predicates, root, criteriaBuilder);
        addCreatedAtRangeFilter(predicates, root, criteriaBuilder);
        // Combine all predicates with AND
        return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    }

    /**
     * Adds origin location search filter if specified.
     * Case-insensitive partial match using LIKE.
     * @param predicates list of predicates to add to
     * @param root root entity
     * @param criteriaBuilder criteria builder
     */
    private void addOriginFilter(List<Predicate> predicates, Root<BikePath> root, CriteriaBuilder criteriaBuilder) {
        if (searchRequest.origin() != null && !searchRequest.origin().isBlank()) {
            String searchPattern = "%" + searchRequest.origin().toLowerCase() + "%";
            predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("origin")),
                    searchPattern
            ));
        }
    }

    /**
     * Adds destination location search filter if specified.
     * Case-insensitive partial match using LIKE.
     * @param predicates list of predicates to add to
     * @param root root entity
     * @param criteriaBuilder criteria builder
     */
    private void addDestinationFilter(List<Predicate> predicates, Root<BikePath> root, CriteriaBuilder criteriaBuilder) {
        if (searchRequest.destination() != null && !searchRequest.destination().isBlank()) {
            String searchPattern = "%" + searchRequest.destination().toLowerCase() + "%";
            predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("destination")),
                    searchPattern
            ));
        }
    }

    /**
     * Adds creation time range filter if specified.
     * Filters bike paths where createdAt between from (inclusive) and to (inclusive).
     * @param predicates list of predicates to add to
     * @param root root entity
     * @param criteriaBuilder criteria builder
     */
    private void addCreatedAtRangeFilter(List<Predicate> predicates, Root<BikePath> root, CriteriaBuilder criteriaBuilder) {
        if (searchRequest.createdAtFrom() != null) {
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    root.get("createdAt"),
                    searchRequest.createdAtFrom()
            ));
        }
        if (searchRequest.createdAtTo() != null) {
            predicates.add(criteriaBuilder.lessThanOrEqualTo(
                    root.get("createdAt"),
                    searchRequest.createdAtTo()
            ));
        }
    }

}