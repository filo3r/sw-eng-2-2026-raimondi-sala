package it.polimi.se.bbp.repository;

import it.polimi.se.bbp.entity.TripPoint;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository interface for TripPoint entity.
 */
public interface TripPointRepository extends JpaRepository<TripPoint, Long> {

}