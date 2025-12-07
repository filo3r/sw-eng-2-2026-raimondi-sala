package it.polimi.se.bbp.repository;

import it.polimi.se.bbp.entity.Trip;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository interface for Trip entity.
 * Provides database access methods for trips.
 */
public interface TripRepository extends JpaRepository<Trip, Long>, JpaSpecificationExecutor<Trip> {

    /**
     * Finds page of trips for a user, eagerly fetching meteorological data.
     * Does NOT fetch TripPoints for efficient pagination.
     * First step in Two-Query Approach for paginated lists.
     * @param userId ID of user who recorded trips
     * @param pageable pagination information (page number, size, sort)
     * @return page of trips with meteorological data loaded
     */
    @Query(value = "SELECT t FROM Trip t LEFT JOIN FETCH t.meteorologicalData WHERE t.recordedBy.id = :userId",
            countQuery = "SELECT COUNT(t) FROM Trip t WHERE t.recordedBy.id = :userId")
    Page<Trip> findPageByRecordedByIdWithWeather(@Param("userId") Long userId, Pageable pageable);

}