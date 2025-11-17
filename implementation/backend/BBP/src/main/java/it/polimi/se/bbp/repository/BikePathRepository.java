package it.polimi.se.bbp.repository;

import it.polimi.se.bbp.entity.BikePath;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository interface for BikePath entity.
 * Provides methods to interact with the bike path database table.
 */
public interface BikePathRepository extends JpaRepository<BikePath, Long> {

    /**
     * Finds all bike paths created by a specific user.
     * Returns both published and private bike paths belonging to the user.
     * @param userId the ID of the user who created the bike paths
     * @return list of bike paths created by the user, ordered by creation date (newest first)
     */
    List<BikePath> findAllByCreatedById(Long userId);

}