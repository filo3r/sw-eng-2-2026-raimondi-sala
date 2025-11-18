package it.polimi.se.bbp.repository;

import it.polimi.se.bbp.entity.Obstacle;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository interface for Obstacle entity.
 * Used for batch insert operations to improve performance.
 */
public interface ObstacleRepository extends JpaRepository<Obstacle, Long> {

}