package it.polimi.se.bbp.repository;

import it.polimi.se.bbp.entity.BikePathPoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository interface for BikePathPoint entity.
 * Supports batch insert operations for performance optimization.
 */
public interface BikePathPointRepository extends JpaRepository<BikePathPoint, Long> {

    /**
     * Finds all bike path points for given bike path IDs.
     * Results ordered by bike path ID, then by sequential position (both ascending).
     * Useful for batch loading points for multiple bike paths.
     * @param bikePathIds list of bike path IDs to fetch points for
     * @return list of bike path points ordered by path ID and sequential position
     */
    List<BikePathPoint> findAllByBikePathIdInOrderByBikePathIdAscSequentialPositionAsc(List<Long> bikePathIds);

}