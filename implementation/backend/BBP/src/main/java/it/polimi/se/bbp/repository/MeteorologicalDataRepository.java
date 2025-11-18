package it.polimi.se.bbp.repository;

import it.polimi.se.bbp.entity.MeteorologicalData;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository interface for MeteorologicalData entity.
 */
public interface MeteorologicalDataRepository extends JpaRepository<MeteorologicalData, Long> {

}