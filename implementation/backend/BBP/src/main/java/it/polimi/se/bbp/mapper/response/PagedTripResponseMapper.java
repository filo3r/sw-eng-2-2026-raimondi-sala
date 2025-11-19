package it.polimi.se.bbp.mapper.response;

import it.polimi.se.bbp.dto.response.PagedTripResponse;
import it.polimi.se.bbp.dto.response.TripResponse;
import it.polimi.se.bbp.entity.Trip;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for converting paginated Trip entities to PagedTripResponse DTOs.
 * Handles pagination metadata extraction and trip data mapping.
 */
@Component
@RequiredArgsConstructor
public class PagedTripResponseMapper {

    /**
     * Mapper for converting individual Trip entities to TripResponse DTOs.
     * Used to map each trip in the page content.
     */
    private final TripResponseMapper tripResponseMapper;

    /**
     * Converts a Spring Data paginated result to a PagedTripResponse DTO.
     * Extracts all pagination metadata from the Page object and maps the trip content.
     * The Page object contains:
     * - Content: List of Trip entities for the current page
     * - Metadata: page number, size, total elements, total pages, etc.
     * @param tripPage the Spring Data Page containing trips and pagination info
     * @return PagedTripResponse with mapped trip content and pagination metadata
     */
    public PagedTripResponse toPagedResponse(Page<Trip> tripPage) {
        // Map each Trip entity to TripResponse DTO
        List<TripResponse> tripResponses = tripPage.getContent().stream()
                .map(tripResponseMapper::toResponse)
                .collect(Collectors.toList());
        // Build PagedTripResponse with content and metadata
        return PagedTripResponse.builder()
                .content(tripResponses)
                .currentPage(tripPage.getNumber())
                .pageSize(tripPage.getSize())
                .totalElements(tripPage.getTotalElements())
                .totalPages(tripPage.getTotalPages())
                .hasNext(tripPage.hasNext())
                .hasPrevious(tripPage.hasPrevious())
                .firstPage(tripPage.isFirst())
                .lastPage(tripPage.isLast())
                .build();
    }

}