package it.polimi.se.bbp.repository;

import it.polimi.se.bbp.entity.BikePath;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
     * WARNING: This loads ALL bike paths at once. Use findIdsByCreatedById for pagination.
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
     * WARNING: This loads ALL bike paths at once. Use findByIdsWithObstacles for pagination.
     * @param userId the ID of the user who created the bike paths
     * @return list of bike paths with obstacles loaded
     */
    @Query("SELECT DISTINCT b FROM BikePath b LEFT JOIN FETCH b.obstacles WHERE b.createdBy.id = :userId")
    List<BikePath> findAllByCreatedByIdWithObstacles(@Param("userId") Long userId);

    /**
     * Finds only the IDs of bike paths created by a specific user (paginated).
     * This is the FIRST step in the 3-step paginated loading strategy:
     * 1. Load page of IDs (this method)
     * 2. Load bike paths with points for those IDs (findByIdsWithPoints)
     * 3. Load obstacles for those IDs (findByIdsWithObstacles)
     * This approach avoids MultipleBagFetchException when using pagination with multiple JOIN FETCH.
     * Used for: getUserBikePaths (paginated version)
     * @param userId the ID of the user who created the bike paths
     * @param pageable pagination information (page number, size, sort)
     * @return page containing only bike path IDs
     */
    @Query("SELECT b.id FROM BikePath b WHERE b.createdBy.id = :userId")
    Page<Long> findIdsByCreatedById(@Param("userId") Long userId, Pageable pageable);

    /**
     * Finds bike paths with bike path points eagerly loaded for specific IDs.
     * This is the SECOND step in the 3-step paginated loading strategy.
     * Loads only the bike paths whose IDs are in the provided list.
     * Returns DISTINCT results to avoid duplicates from JOIN FETCH.
     * The results may not be in the same order as the input IDs - reordering is needed in the service layer.
     * Used for: getUserBikePaths (paginated version)
     * @param ids list of bike path IDs to load
     * @return list of bike paths with points loaded
     */
    @Query("SELECT DISTINCT b FROM BikePath b LEFT JOIN FETCH b.bikePathPoints WHERE b.id IN :ids")
    List<BikePath> findByIdsWithPoints(@Param("ids") List<Long> ids);

    /**
     * Finds bike paths with obstacles eagerly loaded for specific IDs.
     * This is the THIRD step in the 3-step paginated loading strategy.
     * Loads only the bike paths whose IDs are in the provided list.
     * Hibernate will merge the obstacles into the already loaded bike path entities.
     * Returns DISTINCT results to avoid duplicates from JOIN FETCH.
     * Used for: getUserBikePaths (paginated version)
     * @param ids list of bike path IDs to load obstacles for
     * @return list of bike paths with obstacles loaded
     */
    @Query("SELECT DISTINCT b FROM BikePath b LEFT JOIN FETCH b.obstacles WHERE b.id IN :ids")
    List<BikePath> findByIdsWithObstacles(@Param("ids") List<Long> ids);

    /**
     * Finds published bike paths within geographic bounding boxes.
     * Fast filtering at database level using indexed lat/lon columns.
     * Returns candidates that may include false positives (rectangular approximation).
     * Precise filtering with Haversine distance is done in the service layer.
     * Used for: geographic search (step 2 of 3-step search strategy)
     * @param minOriginLat minimum latitude for origin bounding box
     * @param maxOriginLat maximum latitude for origin bounding box
     * @param minOriginLon minimum longitude for origin bounding box
     * @param maxOriginLon maximum longitude for origin bounding box
     * @param minDestLat minimum latitude for destination bounding box
     * @param maxDestLat maximum latitude for destination bounding box
     * @param minDestLon minimum longitude for destination bounding box
     * @param maxDestLon maximum longitude for destination bounding box
     * @return list of candidate bike paths within bounding boxes
     */
    @Query("SELECT b FROM BikePath b WHERE b.published = true AND b.originLatitude BETWEEN :minOriginLat AND :maxOriginLat " +
            "AND b.originLongitude BETWEEN :minOriginLon AND :maxOriginLon AND b.destinationLatitude BETWEEN :minDestLat AND :maxDestLat " +
            "AND b.destinationLongitude BETWEEN :minDestLon AND :maxDestLon")
    List<BikePath> findPublishedWithinBoundingBoxes(
            @Param("minOriginLat") Double minOriginLat,
            @Param("maxOriginLat") Double maxOriginLat,
            @Param("minOriginLon") Double minOriginLon,
            @Param("maxOriginLon") Double maxOriginLon,
            @Param("minDestLat") Double minDestLat,
            @Param("maxDestLat") Double maxDestLat,
            @Param("minDestLon") Double minDestLon,
            @Param("maxDestLon") Double maxDestLon
    );

}