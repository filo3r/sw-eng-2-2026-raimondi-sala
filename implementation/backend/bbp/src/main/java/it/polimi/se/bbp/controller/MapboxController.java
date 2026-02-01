package it.polimi.se.bbp.controller;

import it.polimi.se.bbp.config.mapbox.MapboxConfig;
import it.polimi.se.bbp.dto.request.CyclingRouteRequest;
import it.polimi.se.bbp.dto.request.ForwardGeocodeRequest;
import it.polimi.se.bbp.dto.request.ReverseGeocodeRequest;
import it.polimi.se.bbp.dto.response.CyclingRouteResponse;
import it.polimi.se.bbp.dto.response.GeocodeResponse;
import it.polimi.se.bbp.dto.result.CyclingRouteResult;
import it.polimi.se.bbp.dto.result.GeocodeResult;
import it.polimi.se.bbp.mapper.response.CyclingRouteResponseMapper;
import it.polimi.se.bbp.mapper.response.GeocodeResponseMapper;
import it.polimi.se.bbp.service.mapbox.MapboxService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for Mapbox services.
 * Provides geocoding services.
 */
@RestController
@RequestMapping("/api/mapbox")
@RequiredArgsConstructor
public class MapboxController {

    /**
     * Configuration containing Mapbox API credentials.
     */
    private final MapboxConfig mapboxConfig;

    /**
     * Service for Mapbox API interactions.
     */
    private final MapboxService mapboxService;

    /**
     * Mapper for converting geocode results to response DTOs.
     */
    private final GeocodeResponseMapper geocodeResponseMapper;

    /**
     * Mapper for converting route results to response DTOs.
     */
    private final CyclingRouteResponseMapper cyclingRouteResponseMapper;

    /**
     * Converts an address to geographic coordinates (forward geocoding).
     * Requires authentication.
     * @param request forward geocoding request containing the address
     * @return geocode response with formatted address and coordinates
     */
    @PostMapping("/geocode/forward")
    public ResponseEntity<GeocodeResponse> forwardGeocode(@Valid @RequestBody ForwardGeocodeRequest request) {
        GeocodeResult result = mapboxService.geocodeAddress(request.address());
        GeocodeResponse response = geocodeResponseMapper.toResponse(result);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * Converts geographic coordinates to address (reverse geocoding).
     * Requires authentication.
     * @param request reverse geocoding request containing coordinates
     * @return geocode response with formatted address and coordinates
     */
    @PostMapping("/geocode/reverse")
    public ResponseEntity<GeocodeResponse> reverseGeocode(@Valid @RequestBody ReverseGeocodeRequest request) {
        GeocodeResult result = mapboxService.geocodeCoordinate(request.coordinate());
        GeocodeResponse response = geocodeResponseMapper.toResponse(result);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * Calculates cycling route through multiple waypoints.
     * Requires authentication.
     * @param request route request containing ordered waypoints
     * @return route response with ordered points
     */
    @PostMapping("/cycling-route")
    public ResponseEntity<CyclingRouteResponse> calculateRoute(@Valid @RequestBody CyclingRouteRequest request) {
        CyclingRouteResult result = mapboxService.calculateCyclingRoute(request.waypoints());
        CyclingRouteResponse response = cyclingRouteResponseMapper.toResponse(result);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

}