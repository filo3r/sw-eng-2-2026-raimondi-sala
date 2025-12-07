package it.polimi.se.bbp.mapper.response;

import it.polimi.se.bbp.dto.response.TripPointResponse;
import it.polimi.se.bbp.entity.TripPoint;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

/**
 * Mapper for converting TripPoint entities to TripPointResponse DTOs.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface TripPointResponseMapper {

    /**
     * Converts TripPoint entity to TripPointResponse DTO.
     * @param tripPoint trip point entity
     * @return trip point response DTO
     */
    TripPointResponse toResponse(TripPoint tripPoint);

    /**
     * Converts list of TripPoint entities to list of TripPointResponse DTOs.
     * @param tripPoints list of trip point entities
     * @return list of trip point response DTOs
     */
    List<TripPointResponse> toResponses(List<TripPoint> tripPoints);

}