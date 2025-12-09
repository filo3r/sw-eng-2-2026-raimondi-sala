package it.polimi.se.bbp.repository;

import it.polimi.se.bbp.entity.BikePath;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repository interface for BikePath entity.
 * Provides database access methods for bike paths.
 */
public interface BikePathRepository extends JpaRepository<BikePath, Long>, JpaSpecificationExecutor<BikePath> {

    /**
     * Finds page of bike paths for a user.
     * Does NOT fetch BikePathPoints or Obstacles for efficient pagination.
     * First step in Two-Query Approach for paginated lists.
     * @param userId ID of user who created bike paths
     * @param pageable pagination information (page number, size, sort)
     * @return page of bike paths without relationships loaded
     */
    @Query(value = "SELECT b FROM BikePath b WHERE b.createdBy.id = :userId",
            countQuery = "SELECT COUNT(b) FROM BikePath b WHERE b.createdBy.id = :userId")
    Page<BikePath> findPageByCreatedById(@Param("userId") Long userId, Pageable pageable);

    /**
     * Finds published bike paths within geographic bounding boxes.
     * Fast filtering using indexed lat/lon columns (rectangular approximation).
     * May include false positives, precise Haversine filtering done in service layer.
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