package it.polimi.se.bbp.mapper.entity;

import it.polimi.se.bbp.dto.result.GeocodeResult;
import it.polimi.se.bbp.dto.request.BikePathManualCreateRequest;
import it.polimi.se.bbp.entity.BikePath;
import it.polimi.se.bbp.entity.User;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Mapper for converting bike path request data to BikePath entities.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        builder = @Builder(disableBuilder = true))
public interface BikePathMapper {

    /**
     * Converts BikePathManualCreateRequest to BikePath entity.
     * Receives pre-calculated values from service layer.
     * @param request bike path creation request
     * @param createdBy user creating the bike path
     * @param createdAt creation timestamp
     * @param updatedBy user updating the bike path (can be null)
     * @param updatedAt update timestamp (can be null)
     * @param origin geocoded origin location
     * @param destination geocoded destination location
     * @param score calculated quality score (0.0 - 5.0)
     * @param totalDistanceKm calculated total distance in kilometers
     * @return bike path entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdBy", source = "createdBy")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedBy", source = "updatedBy")
    @Mapping(target = "updatedAt", source = "updatedAt")
    @Mapping(target = "origin", source = "origin.address")
    @Mapping(target = "originLatitude", source = "origin.coordinate.latitude")
    @Mapping(target = "originLongitude", source = "origin.coordinate.longitude")
    @Mapping(target = "destination", source = "destination.address")
    @Mapping(target = "destinationLatitude", source = "destination.coordinate.latitude")
    @Mapping(target = "destinationLongitude", source = "destination.coordinate.longitude")
    @Mapping(target = "description", source = "request.description")
    @Mapping(target = "status", source = "request.status")
    @Mapping(target = "published", source = "request.published")
    @Mapping(target = "score", source = "score")
    @Mapping(target = "totalDistance", source = "totalDistanceKm")
    @Mapping(target = "bikePathPoints", ignore = true)
    @Mapping(target = "obstacles", ignore = true)
    BikePath toEntity(BikePathManualCreateRequest request, User createdBy, OffsetDateTime createdAt, User updatedBy,
                      OffsetDateTime updatedAt, GeocodeResult origin, GeocodeResult destination, BigDecimal score,
                      BigDecimal totalDistanceKm);

}