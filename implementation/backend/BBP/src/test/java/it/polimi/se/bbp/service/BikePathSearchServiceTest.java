package it.polimi.se.bbp.service;

import it.polimi.se.bbp.dto.mapbox.Coordinate;
import it.polimi.se.bbp.dto.mapbox.GeocodeResult;
import it.polimi.se.bbp.dto.request.BikePathFinderRequest;
import it.polimi.se.bbp.entity.BikePath;
import it.polimi.se.bbp.repository.BikePathRepository;
import it.polimi.se.bbp.service.mapbox.MapboxService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BikePathSearchServiceTest {

    @Mock private BikePathRepository bikePathRepository;
    @Mock private MapboxService mapboxService;

    @InjectMocks
    private BikePathSearchService searchService;

    @Test
    void searchBikePaths_filtersByDistance() {
        // setup
        BikePathFinderRequest req = new BikePathFinderRequest();
        req.setOriginAddress("origin");
        req.setDestinationAddress("dest");
        req.setOriginRadiusKm(1.0);
        req.setDestinationRadiusKm(1.0);

        // mock geocode
        GeocodeResult geo = new GeocodeResult();
        geo.setCoordinate(new Coordinate(10.0, 10.0));
        when(mapboxService.geocodeAddress(any())).thenReturn(geo);

        // mock db return (2 candidates: one close, one far)
        BikePath good = BikePath.builder().id(1L).score(BigDecimal.TEN)
                .originLatitude(10.0).originLongitude(10.0)
                .destinationLatitude(10.0).destinationLongitude(10.0).build();

        BikePath bad = BikePath.builder().id(2L).score(BigDecimal.ZERO)
                .originLatitude(50.0).originLongitude(50.0) // far away
                .destinationLatitude(50.0).destinationLongitude(50.0).build();

        when(bikePathRepository.findPublishedWithinBoundingBoxes(
                anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                anyDouble(), anyDouble(), anyDouble(), anyDouble()
        )).thenReturn(List.of(good, bad));

        when(bikePathRepository.findByIdsWithPoints(any())).thenReturn(List.of(good));

        // execute
        Page<BikePath> result = searchService.searchBikePaths(req, 0, 10);

        // verify
        assertEquals(1, result.getTotalElements());
        assertEquals(1L, result.getContent().get(0).getId());
    }
}