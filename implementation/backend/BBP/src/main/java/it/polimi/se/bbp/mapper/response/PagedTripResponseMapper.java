package it.polimi.se.bbp.mapper.response;

import it.polimi.se.bbp.dto.response.PagedTripResponse;
import it.polimi.se.bbp.dto.response.TripResponse;
import it.polimi.se.bbp.entity.Trip;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Mapper for converting paginated Trip results to PagedTripResponse DTOs.
 * Extracts pagination metadata and maps content list.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        uses = {TripResponseMapper.class})
public interface PagedTripResponseMapper {

    /**
     * Converts Spring Data Page of trips to PagedTripResponse DTO.
     * Extracts pagination metadata and delegates content mapping to TripResponseMapper.
     * @param tripPage Spring Data Page containing trip entities and pagination info
     * @return paged response DTO with mapped content and pagination metadata
     */
    default PagedTripResponse toPagedResponse(Page<Trip> tripPage) {
        if (tripPage == null)
            return null;
        List<TripResponse> content = mapContent(tripPage.getContent());
        return new PagedTripResponse(
                content,
                tripPage.getNumber(),
                tripPage.getSize(),
                tripPage.getTotalElements(),
                tripPage.getTotalPages(),
                tripPage.hasNext(),
                tripPage.hasPrevious(),
                tripPage.isFirst(),
                tripPage.isLast()
        );
    }

    /**
     * Maps list of Trip entities to TripResponse DTOs.
     * MapStruct generates implementation using TripResponseMapper.
     * @param trips list of Trip entities
     * @return list of TripResponse DTOs
     */
    List<TripResponse> mapContent(List<Trip> trips);

}