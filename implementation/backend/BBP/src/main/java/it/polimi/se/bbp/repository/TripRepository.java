package it.polimi.se.bbp.repository;

import it.polimi.se.bbp.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

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

}