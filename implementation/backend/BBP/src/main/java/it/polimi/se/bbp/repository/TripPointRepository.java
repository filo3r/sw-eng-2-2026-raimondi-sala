package it.polimi.se.bbp.repository;

import it.polimi.se.bbp.entity.TripPoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository interface for TripPoint entity.
 * Provides standard CRUD operations via JpaRepository.
 */
public interface TripPointRepository extends JpaRepository<TripPoint, Long> {

    /**
     * Finds all trip points for given trip IDs.
     * Results ordered by trip ID, then by sequential position (both ascending).
     * Useful for batch loading points for multiple trips.
     * @param tripIds list of trip IDs to fetch points for
     * @return list of trip points ordered by trip ID and sequential position
     */
    List<TripPoint> findAllByTripIdInOrderByTripIdAscSequentialPositionAsc(List<Long> tripIds);

}