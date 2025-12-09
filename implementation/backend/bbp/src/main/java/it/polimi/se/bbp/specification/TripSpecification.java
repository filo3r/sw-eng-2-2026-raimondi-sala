package it.polimi.se.bbp.specification;

import it.polimi.se.bbp.dto.request.TripSearchRequest;
import it.polimi.se.bbp.entity.Trip;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * JPA Specification for building dynamic trip search queries.
 * Implements Specification pattern for type-safe queries using Criteria API.
 * All filters combined with AND logic.
 */
public record TripSpecification(

        /* ID of user who recorded trips */
        Long userId,

        /* Search request with filter criteria */
        TripSearchRequest searchRequest

) implements Specification<Trip> {

    /**
     * Constructs JPA Predicate for search query.
     * Combines all non-null filters with AND logic.
     * Always filters by user ID to ensure users only see their own trips.
     * Eagerly fetches meteorological data to avoid N+1 queries (except for count queries).
     * @param root root entity (Trip)
     * @param query CriteriaQuery being constructed
     * @param criteriaBuilder CriteriaBuilder for creating predicates
     * @return combined Predicate representing all filters
     */
    @Override
    public Predicate toPredicate(Root<Trip> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
        List<Predicate> predicates = new ArrayList<>();
        // Always filter by user ID
        predicates.add(criteriaBuilder.equal(root.get("recordedBy").get("id"), userId));
        addOriginFilter(predicates, root, criteriaBuilder);
        addDestinationFilter(predicates, root, criteriaBuilder);
        addStartTimeRangeFilter(predicates, root, criteriaBuilder);
        // Eagerly fetch meteorological data to avoid N+1 queries (only if not count query)
        if (!isCountQuery(query)) {
            root.fetch("meteorologicalData", JoinType.LEFT);
        }
        return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    }

    /**
     * Adds origin location search filter if specified.
     * Case-insensitive partial match using LIKE.
     * @param predicates list of predicates to add to
     * @param root root entity
     * @param criteriaBuilder criteria builder
     */
    private void addOriginFilter(List<Predicate> predicates, Root<Trip> root, CriteriaBuilder criteriaBuilder) {
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
    private void addDestinationFilter(List<Predicate> predicates, Root<Trip> root, CriteriaBuilder criteriaBuilder) {
        if (searchRequest.destination() != null && !searchRequest.destination().isBlank()) {
            String searchPattern = "%" + searchRequest.destination().toLowerCase() + "%";
            predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("destination")),
                    searchPattern
            ));
        }
    }

    /**
     * Adds start time range filter if specified.
     * Filters trips where startTime between from (inclusive) and to (inclusive).
     * @param predicates list of predicates to add to
     * @param root root entity
     * @param criteriaBuilder criteria builder
     */
    private void addStartTimeRangeFilter(List<Predicate> predicates, Root<Trip> root, CriteriaBuilder criteriaBuilder) {
        if (searchRequest.startTimeFrom() != null) {
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    root.get("startTime"),
                    searchRequest.startTimeFrom()
            ));
        }
        if (searchRequest.startTimeTo() != null) {
            predicates.add(criteriaBuilder.lessThanOrEqualTo(
                    root.get("startTime"),
                    searchRequest.startTimeTo()
            ));
        }
    }

    /**
     * Determines if current query is count query.
     * Count queries should not include fetch joins as they cause issues with JPA.
     * @param query CriteriaQuery being constructed
     * @return true if count query
     */
    private boolean isCountQuery(CriteriaQuery<?> query) {
        return query.getResultType() == Long.class || query.getResultType() == long.class;
    }

}