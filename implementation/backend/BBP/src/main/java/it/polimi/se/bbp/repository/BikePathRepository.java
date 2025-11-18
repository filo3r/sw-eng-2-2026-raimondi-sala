package it.polimi.se.bbp.repository;

import it.polimi.se.bbp.entity.BikePath;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for BikePath entity.
 * Provides methods to interact with the bike path database table.
 */
public interface BikePathRepository extends JpaRepository<BikePath, Long> {

    /**
     * Finds all bike paths created by a specific user.
     * Returns both published and private bike paths belonging to the user.
     * WARNING: This method does NOT eagerly load relationships (LAZY).
     * Use findAllByCreatedByIdWithPoints and findAllByCreatedByIdWithObstacles for complete data.
     * @param userId the ID of the user who created the bike paths
     * @return list of bike paths created by the user, ordered by creation date (newest first)
     */
    List<BikePath> findAllByCreatedById(Long userId);

    /**
     * Finds a bike path by ID with bike path points eagerly loaded.
     * This is the FIRST query to call when you need complete bike path data.
     * After this, call findByIdWithObstacles to load obstacles.
     * @param id the bike path ID
     * @return Optional containing the bike path with points if found
     */
    @Query("SELECT b FROM BikePath b LEFT JOIN FETCH b.bikePathPoints WHERE b.id = :id")
    Optional<BikePath> findByIdWithPoints(@Param("id") Long id);

    /**
     * Finds a bike path by ID with obstacles eagerly loaded.
     * This is the SECOND query to call after findByIdWithPoints.
     * Hibernate will merge the obstacles into the already loaded entity.
     * @param id the bike path ID
     * @return Optional containing the bike path with obstacles if found
     */
    @Query("SELECT b FROM BikePath b LEFT JOIN FETCH b.obstacles WHERE b.id = :id")
    Optional<BikePath> findByIdWithObstacles(@Param("id") Long id);

    /**
     * Finds all bike paths created by a specific user with bike path points eagerly loaded.
     * This is the FIRST query to call when loading user's bike paths.
     * After this, call findAllByCreatedByIdWithObstacles to load obstacles.
     * Returns DISTINCT results to avoid duplicates from JOIN FETCH.
     * @param userId the ID of the user who created the bike paths
     * @return list of bike paths with points loaded, ordered by creation date (newest first)
     */
    @Query("SELECT DISTINCT b FROM BikePath b LEFT JOIN FETCH b.bikePathPoints WHERE b.createdBy.id = :userId " +
            "ORDER BY b.createdAt DESC")
    List<BikePath> findAllByCreatedByIdWithPoints(@Param("userId") Long userId);

    /**
     * Finds all bike paths created by a specific user with obstacles eagerly loaded.
     * This is the SECOND query to call after findAllByCreatedByIdWithPoints.
     * Hibernate will merge the obstacles into the already loaded entities.
     * Returns DISTINCT results to avoid duplicates from JOIN FETCH.
     * @param userId the ID of the user who created the bike paths
     * @return list of bike paths with obstacles loaded
     */
    @Query("SELECT DISTINCT b FROM BikePath b LEFT JOIN FETCH b.obstacles WHERE b.createdBy.id = :userId")
    List<BikePath> findAllByCreatedByIdWithObstacles(@Param("userId") Long userId);

}