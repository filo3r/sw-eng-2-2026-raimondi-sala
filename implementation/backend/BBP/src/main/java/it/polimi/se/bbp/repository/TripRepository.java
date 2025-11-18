package it.polimi.se.bbp.repository;

import it.polimi.se.bbp.entity.Trip;
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
     * Used for: getUserTrips
     * @param userId the ID of the user who recorded the trips
     * @return list of trips with all relationships loaded
     */
    @Query("SELECT DISTINCT t FROM Trip t LEFT JOIN FETCH t.tripPoints LEFT JOIN FETCH t.meteorologicalData " +
            "WHERE t.recordedBy.id = :userId ORDER BY t.startTime DESC")
    List<Trip> findAllByRecordedByIdWithPointsAndWeather(@Param("userId") Long userId);

}