package it.polimi.se.bbp.service;

import it.polimi.se.bbp.dto.request.BikePathFinderRequest;
import it.polimi.se.bbp.dto.result.GeocodeResult;
import it.polimi.se.bbp.entity.BikePath;
import it.polimi.se.bbp.entity.BikePathPoint;
import it.polimi.se.bbp.entity.Obstacle;
import it.polimi.se.bbp.geo.Coordinate;
import it.polimi.se.bbp.repository.BikePathPointRepository;
import it.polimi.se.bbp.repository.BikePathRepository;
import it.polimi.se.bbp.repository.ObstacleRepository;
import it.polimi.se.bbp.service.mapbox.MapboxService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Bike Path Finder Service Tests")
class BikePathFinderServiceTest {

    @Mock
    private BikePathRepository bikePathRepository;
    @Mock
    private BikePathPointRepository bikePathPointRepository;
    @Mock
    private ObstacleRepository obstacleRepository;
    @Mock
    private MapboxService mapboxService;

    @InjectMocks
    private BikePathFinderService bikePathFinderService;

    @Test
    @DisplayName("Should find, filter, and sort bike paths correctly within geographic range")
    void findBikePaths_Success() {
        // 1. Setup Request
        BikePathFinderRequest request = new BikePathFinderRequest(
                "Milan", 10.0,
                "Monza", 10.0
        );

        // 2. Mock Geocoding
        // Origin: Milan (45.4642, 9.1900)
        Coordinate originCoord = new Coordinate(45.4642, 9.1900);
        when(mapboxService.geocodeAddress("Milan"))
                .thenReturn(new GeocodeResult("Milan, Italy", originCoord));

        // Destination: Monza (45.5845, 9.2744)
        Coordinate destCoord = new Coordinate(45.5845, 9.2744);
        when(mapboxService.geocodeAddress("Monza"))
                .thenReturn(new GeocodeResult("Monza, Italy", destCoord));

        // 3. Setup Candidates
        // Path 1: Valid, High Score (5.0), within range
        BikePath path1 = new BikePath();
        path1.setId(1L);
        path1.setScore(new BigDecimal("5.0"));
        path1.setOriginLatitude(45.4650); // Very close to Milan
        path1.setOriginLongitude(9.1910);
        path1.setDestinationLatitude(45.5850); // Very close to Monza
        path1.setDestinationLongitude(9.2750);
        path1.setPublished(true);

        // Path 2: Valid, Lower Score (3.0), within range
        BikePath path2 = new BikePath();
        path2.setId(2L);
        path2.setScore(new BigDecimal("3.0"));
        path2.setOriginLatitude(45.4660);
        path2.setOriginLongitude(9.1920);
        path2.setDestinationLatitude(45.5860);
        path2.setDestinationLongitude(9.2760);
        path2.setPublished(true);

        // Path 3: Invalid (Origin too far, e.g., Rome), should be filtered out by precise logic
        BikePath path3 = new BikePath();
        path3.setId(3L);
        path3.setScore(new BigDecimal("4.0"));
        path3.setOriginLatitude(41.9028); // Rome
        path3.setOriginLongitude(12.4964);
        path3.setDestinationLatitude(45.5845); // Monza
        path3.setDestinationLongitude(9.2744);
        path3.setPublished(true);

        // Mock Bounding Box Query results
        // Even if Rome is strictly outside the bounding box of Milan, we simulate the repository returning it
        // to verify that the service's "Step 4: Filter with precise Haversine distance" works correctly.
        when(bikePathRepository.findPublishedWithinBoundingBoxes(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of(path1, path2, path3));

        // 4. Mock Loading Complete Paths
        // Expect loading only for path1 and path2 IDs. The service should exclude path3.
        // It should request them in the order of score (5.0 then 3.0) -> IDs [1, 2]
        when(bikePathRepository.findAllById(anyList()))
                .thenReturn(List.of(path1, path2));

        // 5. Mock Relationships (Points and Obstacles)
        when(bikePathPointRepository.findAllByBikePathIdInOrderByBikePathIdAscSequentialPositionAsc(anyList()))
                .thenReturn(Collections.emptyList());
        when(obstacleRepository.findAllByBikePathIdInOrderByBikePathIdAscPositionOnPathAsc(anyList()))
                .thenReturn(Collections.emptyList());

        // 6. Execute
        Page<BikePath> result = bikePathFinderService.findBikePaths(request, 0, 5);

        // 7. Verify
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());

        // Verify sorting by score descending: Path 1 (5.0) should be first, Path 2 (3.0) second
        assertEquals(1L, result.getContent().get(0).getId());
        assertEquals(2L, result.getContent().get(1).getId());

        // Verify Path 3 was filtered out
        assertTrue(result.getContent().stream().noneMatch(bp -> bp.getId().equals(3L)));

        // Verify interactions
        verify(mapboxService, times(2)).geocodeAddress(anyString());
        verify(bikePathRepository).findPublishedWithinBoundingBoxes(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when pagination parameters are invalid")
    void findBikePaths_InvalidPagination() {
        BikePathFinderRequest request = new BikePathFinderRequest("A", 1.0, "B", 1.0);

        // Negative page
        assertThrows(IllegalArgumentException.class, () ->
                bikePathFinderService.findBikePaths(request, -1, 10));

        // Zero or negative size
        assertThrows(IllegalArgumentException.class, () ->
                bikePathFinderService.findBikePaths(request, 0, 0));

        // Size larger than MAX_PAGE_SIZE (10)
        assertThrows(IllegalArgumentException.class, () ->
                bikePathFinderService.findBikePaths(request, 0, 11));
    }

    @Test
    @DisplayName("Should return empty page when no matching bike paths are found")
    void findBikePaths_NoResults() {
        BikePathFinderRequest request = new BikePathFinderRequest("Unknown", 5.0, "Unknown", 5.0);

        when(mapboxService.geocodeAddress(anyString()))
                .thenReturn(new GeocodeResult("Loc", new Coordinate(0.0, 0.0)));

        // Repository returns empty list
        when(bikePathRepository.findPublishedWithinBoundingBoxes(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(Collections.emptyList());

        Page<BikePath> result = bikePathFinderService.findBikePaths(request, 0, 5);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());
    }
}