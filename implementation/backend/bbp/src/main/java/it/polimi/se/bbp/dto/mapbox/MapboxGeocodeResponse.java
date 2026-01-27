package it.polimi.se.bbp.dto.mapbox;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.polimi.se.bbp.exception.mapbox.InvalidAddressException;
import it.polimi.se.bbp.exception.mapbox.InvalidCoordinateException;
import it.polimi.se.bbp.exception.mapbox.MapboxApiException;
import it.polimi.se.bbp.geo.Coordinate;
import it.polimi.se.bbp.dto.result.GeocodeResult;

import java.util.List;

/**
 * Raw JSON response from Mapbox Search Box API (Forward and Reverse).
 * Follows GeoJSON FeatureCollection structure.
 * Ignores unknown properties for forward compatibility.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MapboxGeocodeResponse(

        /* List of GeoJSON features returned by search. Can be empty if no results found. */
        @JsonProperty("features")
        List<Feature> features

) {

    /**
     * Single GeoJSON Feature representing a location result.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Feature(

            /* Geometry object containing geographic coordinates. */
            @JsonProperty("geometry")
            Geometry geometry,

            /* Properties object containing location metadata (name, formatted addresses). */
            @JsonProperty("properties")
            Properties properties

    ) {}

    /**
     * GeoJSON Point geometry with coordinate accessors.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Geometry(

            /* List of coordinate values in GeoJSON format [longitude, latitude]. */
            @JsonProperty("coordinates")
            List<Double> coordinates

    ) {

        /**
         * Retrieves longitude from coordinates.
         * @return longitude value (index 0), or null if insufficient data
         */
        public Double getLongitude() {
            return coordinates != null && coordinates.size() >= 1 ? coordinates.get(0) : null;
        }

        /**
         * Retrieves latitude from coordinates.
         * @return latitude value (index 1), or null if insufficient data
         */
        public Double getLatitude() {
            return coordinates != null && coordinates.size() >= 2 ? coordinates.get(1) : null;
        }
    }

    /**
     * Properties object containing location metadata.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Properties(

            /* Complete formatted address string. Example: "34170 Gannon Terrace, Fremont, California 94555, United States". Optional, may be null. */
            @JsonProperty("full_address")
            String fullAddress,

            /* Primary name of the location. Example: "34170 Gannon Terrace". Generally guaranteed by Mapbox. */
            @JsonProperty("name")
            String name,

            /* Formatted place context (city, state, zip, country). Example: "Fremont, California 94555, United States". Optional, may be null. */
            @JsonProperty("place_formatted")
            String placeFormatted

    ) {

        /**
         * Determines best available address string.
         * Priority: fullAddress if present, else name + placeFormatted, else name only.
         * @return valid address string, or null if all fields missing or blank
         */
        public String getBestAddress() {
            if (fullAddress != null && !fullAddress.isBlank())
                return fullAddress;
            // Fallback: construct address from name + context
            if (name != null && !name.isBlank()) {
                if (placeFormatted != null && !placeFormatted.isBlank())
                    return name + ", " + placeFormatted;
                return name;
            }
            return null;
        }
    }

    /**
     * Converts forward geocoding response to GeocodeResult.
     * @param address original address string that was searched
     * @return validated geocode result with best match address and coordinates
     * @throws InvalidAddressException if address not found
     * @throws MapboxApiException if API response is malformed or missing required data
     */
    public GeocodeResult toGeocodeResult(String address) {
        if (features == null || features.isEmpty())
            throw new InvalidAddressException(address);
        return extractGeocodeResult();
    }

    /**
     * Converts reverse geocoding response to GeocodeResult.
     * @param coordinate original coordinates that were reverse geocoded
     * @return validated geocode result with best match address and coordinates
     * @throws InvalidCoordinateException if no address found for coordinates
     * @throws MapboxApiException if API response is malformed or missing required data
     */
    public GeocodeResult toGeocodeResult(Coordinate coordinate) {
        if (features == null || features.isEmpty())
            throw new InvalidCoordinateException(coordinate);
        return extractGeocodeResult();
    }

    /**
     * Internal method to extract and validate geocode result from first feature.
     * Shared logic for both forward and reverse geocoding.
     * @return validated GeocodeResult
     * @throws MapboxApiException if response is malformed or missing required data
     */
    private GeocodeResult extractGeocodeResult() {
        Feature firstFeature = features.getFirst();
        // Validate feature existence - technical error if null
        if (firstFeature == null)
            throw new MapboxApiException("First feature is null in Mapbox response");
        // Validate geometry and coordinates - technical error
        if (firstFeature.geometry() == null)
            throw new MapboxApiException("Invalid geometry: missing geometry object");
        Double lat = firstFeature.geometry().getLatitude();
        Double lon = firstFeature.geometry().getLongitude();
        if (lat == null || lon == null)
            throw new MapboxApiException("Invalid geometry: missing coordinates values");
        // Validate properties - technical error
        if (firstFeature.properties() == null)
            throw new MapboxApiException("Invalid response: missing properties object");
        String bestAddress = firstFeature.properties().getBestAddress();
        // Final validity check - technical error
        if (bestAddress == null || bestAddress.isBlank())
            throw new MapboxApiException("No valid address string found in Mapbox response properties");
        return new GeocodeResult(bestAddress, new Coordinate(lat, lon));
    }

}