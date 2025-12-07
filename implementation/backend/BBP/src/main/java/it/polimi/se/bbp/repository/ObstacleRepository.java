package it.polimi.se.bbp.repository;

import it.polimi.se.bbp.entity.Obstacle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository interface for Obstacle entity.
 * Supports batch insert operations for performance optimization.
 */
public interface ObstacleRepository extends JpaRepository<Obstacle, Long> {

    /**
     * Finds all obstacles for multiple bike paths.
     * Results ordered by bike path ID, then by position on path (both ascending).
     * Used for batch loading when retrieving paginated bike paths.
     * @param bikePathIds list of bike path IDs
     * @return list of obstacles ordered by bikePathId and positionOnPath
     */
    List<Obstacle> findAllByBikePathIdInOrderByBikePathIdAscPositionOnPathAsc(List<Long> bikePathIds);

}