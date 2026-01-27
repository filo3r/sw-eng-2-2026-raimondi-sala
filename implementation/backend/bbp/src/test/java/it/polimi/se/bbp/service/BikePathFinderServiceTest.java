package it.polimi.se.bbp.service;

import it.polimi.se.bbp.dto.request.BikePathFinderRequest;
import it.polimi.se.bbp.dto.result.GeocodeResult;
import it.polimi.se.bbp.entity.BikePath;
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
        // We initialize a request to search for paths between Milan and Monza within a 10km range
        BikePathFinderRequest request = new BikePathFinderRequest(
                "Milan", 10.0,
                "Monza", 10.0
        );

        // We simulate the geocoding service returning valid coordinates for both the origin  and destination
        Coordinate originCoord = new Coordinate(45.4642, 9.1900);
        when(mapboxService.geocodeAddress("Milan"))
                .thenReturn(new GeocodeResult("Milan, Italy", originCoord));

        Coordinate destCoord = new Coordinate(45.5845, 9.2744);
        when(mapboxService.geocodeAddress("Monza"))
                .thenReturn(new GeocodeResult("Monza, Italy", destCoord));

        // Path 1: Valid and highly rated, geographically close to the request points
        BikePath path1 = new BikePath();
        path1.setId(1L);
        path1.setScore(new BigDecimal("5.0"));
        path1.setOriginLatitude(45.4650);
        path1.setOriginLongitude(9.1910);
        path1.setDestinationLatitude(45.5850);
        path1.setDestinationLongitude(9.2750);
        path1.setPublished(true);

        // Path 2: Valid but lower rated, also within range
        BikePath path2 = new BikePath();
        path2.setId(2L);
        path2.setScore(new BigDecimal("3.0"));
        path2.setOriginLatitude(45.4660);
        path2.setOriginLongitude(9.1920);
        path2.setDestinationLatitude(45.5860);
        path2.setDestinationLongitude(9.2760);
        path2.setPublished(true);

        // Path 3: Invalid because it is too far, intended to test the filtering logic
        BikePath path3 = new BikePath();
        path3.setId(3L);
        path3.setScore(new BigDecimal("4.0"));
        path3.setOriginLatitude(41.9028);
        path3.setOriginLongitude(12.4964);
        path3.setDestinationLatitude(45.5845);
        path3.setDestinationLongitude(9.2744);
        path3.setPublished(true);

        // We simulate the bounding box query returning all paths, including the distant one,
        // to verify that the service correctly filters out Path 3 using Haversine distance
        when(bikePathRepository.findPublishedWithinBoundingBoxes(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of(path1, path2, path3));

        // The service should proceed to load full details only for the valid paths ,
        // effectively excluding Path 3 before this step
        when(bikePathRepository.findAllById(anyList()))
                .thenReturn(List.of(path1, path2));

        // We ensure that points and obstacles are loaded for the resulting paths
        when(bikePathPointRepository.findAllByBikePathIdInOrderByBikePathIdAscSequentialPositionAsc(anyList()))
                .thenReturn(Collections.emptyList());
        when(obstacleRepository.findAllByBikePathIdInOrderByBikePathIdAscPositionOnPathAsc(anyList()))
                .thenReturn(Collections.emptyList());

        Page<BikePath> result = bikePathFinderService.findBikePaths(request, 0, 5);

        // The result should contain exactly 2 paths, sorted by score descending,
        // and Path 3 should be completely absent
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());

        assertEquals(1L, result.getContent().get(0).getId());
        assertEquals(2L, result.getContent().get(1).getId());

        assertTrue(result.getContent().stream().noneMatch(bp -> bp.getId().equals(3L)));

        verify(mapboxService, times(2)).geocodeAddress(anyString());
        verify(bikePathRepository).findPublishedWithinBoundingBoxes(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when pagination parameters are invalid")
    void findBikePaths_InvalidPagination() {
        BikePathFinderRequest request = new BikePathFinderRequest("A", 1.0, "B", 1.0);

        // Negative page numbers should be rejected
        assertThrows(IllegalArgumentException.class, () ->
                bikePathFinderService.findBikePaths(request, -1, 10));

        // Page size must be strictly positive
        assertThrows(IllegalArgumentException.class, () ->
                bikePathFinderService.findBikePaths(request, 0, 0));

        // Page size cannot exceed the defined maximum
        assertThrows(IllegalArgumentException.class, () ->
                bikePathFinderService.findBikePaths(request, 0, 11));
    }

    @Test
    @DisplayName("Should return empty page when no matching bike paths are found")
    void findBikePaths_NoResults() {
        BikePathFinderRequest request = new BikePathFinderRequest("Unknown", 5.0, "Unknown", 5.0);

        when(mapboxService.geocodeAddress(anyString()))
                .thenReturn(new GeocodeResult("Loc", new Coordinate(0.0, 0.0)));

        // If the repository returns no paths within the bounding box, the service should return an empty page gracefully
        when(bikePathRepository.findPublishedWithinBoundingBoxes(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(Collections.emptyList());

        Page<BikePath> result = bikePathFinderService.findBikePaths(request, 0, 5);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());
    }
}