package it.polimi.se.bbp.mapper.mapbox;

import it.polimi.se.bbp.dto.mapbox.Coordinate;
import org.springframework.stereotype.Component;

/**
 * Mapper for creating Coordinate DTOs.
 */
@Component
public class CoordinateMapper {

    /**
     * Creates a Coordinate DTO from latitude and longitude values.
     * @param latitude the latitude in decimal degrees
     * @param longitude the longitude in decimal degrees
     * @return the coordinate DTO
     */
    public Coordinate toCoordinate(Double latitude, Double longitude) {
        return Coordinate.builder()
                .latitude(latitude)
                .longitude(longitude)
                .build();
    }

}