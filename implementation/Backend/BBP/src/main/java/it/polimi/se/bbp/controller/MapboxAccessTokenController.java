package it.polimi.se.bbp.controller;

import it.polimi.se.bbp.config.mapbox.MapboxConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST controller for Mapbox access token management.
 * Provides the Mapbox API access token for client-side map rendering.
 */
@RestController
@RequestMapping("/api/mapbox")
@RequiredArgsConstructor
public class MapboxAccessTokenController {

    /**
     * Configuration containing Mapbox API credentials.
     */
    private final MapboxConfig mapboxConfig;

    /**
     * Returns the Mapbox access token for client-side usage.
     * Used by frontend applications to render interactive maps and use Mapbox services.
     * @return map containing the access token
     */
    @GetMapping("/access-token")
    public ResponseEntity<Map<String, String>> getAccessToken() {
        return ResponseEntity.ok(Map.of("mapboxAccessToken", mapboxConfig.getApiKey()));
    }

}