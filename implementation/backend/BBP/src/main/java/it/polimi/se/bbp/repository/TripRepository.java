package it.polimi.se.bbp.repository;

import it.polimi.se.bbp.entity.Trip;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Trip entity.
 * Provides methods to interact with the trip database table.
 */
public interface TripRepository extends JpaRepository<Trip, Long> {

    /**
     * Finds all trips recorded by a specific user.
     * WARNING: This method does NOT eagerly load relationships (LAZY).
     * Use findAllByRecordedByIdWithPointsAndWeather for complete data.
     * @param userId the ID of the user who recorded the trips
     * @return list of trips belonging to the user
     */
    List<Trip> findAllByRecordedById(Long userId);

    /**
     * Finds a trip by ID with trip points and meteorological data eagerly loaded.
     * This method uses JOIN FETCH to avoid N+1 query problem and ensure
     * all related entities are loaded in a single query.
     * Used for: recordTripManual (reload)
     * @param id the trip ID
     * @return Optional containing the trip with points and weather data if found
     */
    @Query("SELECT t FROM Trip t LEFT JOIN FETCH t.tripPoints LEFT JOIN FETCH t.meteorologicalData WHERE t.id = :id")
    Optional<Trip> findByIdWithPointsAndWeather(@Param("id") Long id);

    /**
     * Finds all trips recorded by a specific user with points and meteorological data eagerly loaded.
     * This method uses JOIN FETCH to avoid N+1 query problem.
     * WARNING: This loads ALL trips at once. Use findPageByRecordedByIdWithPointsAndWeather for pagination.
     * Used for: getUserTrips (non-paginated version)
     * @param userId the ID of the user who recorded the trips
     * @return list of trips with all relationships loaded
     */
    @Query("SELECT DISTINCT t FROM Trip t LEFT JOIN FETCH t.tripPoints LEFT JOIN FETCH t.meteorologicalData " +
            "WHERE t.recordedBy.id = :userId ORDER BY t.startTime DESC")
    List<Trip> findAllByRecordedByIdWithPointsAndWeather(@Param("userId") Long userId);

    /**
     * Finds a page of trips recorded by a specific user with points and meteorological data eagerly loaded.
     * This method uses JOIN FETCH to avoid N+1 query problem.
     * OPTIMIZED: Returns only the requested page of trips instead of loading all trips.
     * The DISTINCT keyword prevents duplicate results from JOIN FETCH operations.
     * Used for: getUserTrips (paginated version)
     * @param userId the ID of the user who recorded the trips
     * @param pageable pagination information (page number, size, sort)
     * @return page of trips with all relationships loaded
     */
    @Query(value = "SELECT DISTINCT t FROM Trip t LEFT JOIN FETCH t.tripPoints LEFT JOIN FETCH t.meteorologicalData " +
            "WHERE t.recordedBy.id = :userId",
            countQuery = "SELECT COUNT(DISTINCT t) FROM Trip t WHERE t.recordedBy.id = :userId")
    Page<Trip> findPageByRecordedByIdWithPointsAndWeather(@Param("userId") Long userId, Pageable pageable);

}