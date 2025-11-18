package it.polimi.se.bbp.repository;

import it.polimi.se.bbp.entity.BikePathPoint;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository interface for BikePathPoint entity.
 * Used for batch insert operations to improve performance.
 */
public interface BikePathPointRepository extends JpaRepository<BikePathPoint, Long> {

}