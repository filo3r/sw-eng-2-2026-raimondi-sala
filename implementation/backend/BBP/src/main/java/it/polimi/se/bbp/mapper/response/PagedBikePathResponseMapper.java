package it.polimi.se.bbp.mapper.response;

import it.polimi.se.bbp.dto.response.BikePathResponse;
import it.polimi.se.bbp.dto.response.PagedBikePathResponse;
import it.polimi.se.bbp.entity.BikePath;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for converting paginated BikePath entities to PagedBikePathResponse DTOs.
 * Handles pagination metadata extraction and bike path data mapping.
 */
@Component
@RequiredArgsConstructor
public class PagedBikePathResponseMapper {

    /**
     * Mapper for converting individual BikePath entities to BikePathResponse DTOs.
     * Used to map each bike path in the page content.
     */
    private final BikePathResponseMapper bikePathResponseMapper;

    /**
     * Converts a Spring Data paginated result to a PagedBikePathResponse DTO.
     * Extracts all pagination metadata from the Page object and maps the bike path content.
     * The Page object contains:
     * - Content: List of BikePath entities for the current page
     * - Metadata: page number, size, total elements, total pages, etc.
     * @param bikePathPage the Spring Data Page containing bike paths and pagination info
     * @return PagedBikePathResponse with mapped bike path content and pagination metadata
     */
    public PagedBikePathResponse toPagedResponse(Page<BikePath> bikePathPage) {
        // Map each BikePath entity to BikePathResponse DTO
        List<BikePathResponse> bikePathResponses = bikePathPage.getContent().stream()
                .map(bikePathResponseMapper::toResponse)
                .collect(Collectors.toList());
        // Build PagedBikePathResponse with content and metadata
        return PagedBikePathResponse.builder()
                .content(bikePathResponses)
                .currentPage(bikePathPage.getNumber())
                .pageSize(bikePathPage.getSize())
                .totalElements(bikePathPage.getTotalElements())
                .totalPages(bikePathPage.getTotalPages())
                .hasNext(bikePathPage.hasNext())
                .hasPrevious(bikePathPage.hasPrevious())
                .firstPage(bikePathPage.isFirst())
                .lastPage(bikePathPage.isLast())
                .build();
    }

    /**
     * Converts a list of BikePath entities to a PagedBikePathResponse DTO with manual pagination.
     * This method is used when pagination is handled manually (e.g., 3-step loading strategy).
     * Creates a Page object from the list and pageable, then delegates to the main mapping method.
     * @param bikePaths the list of BikePath entities for the current page
     * @param pageable the Pageable object containing page number, size, and sort information
     * @param totalElements the total number of bike paths across all pages
     * @return PagedBikePathResponse with mapped bike path content and pagination metadata
     */
    public PagedBikePathResponse toPagedResponse(List<BikePath> bikePaths, Pageable pageable, long totalElements) {
        // Create a Page object manually from the list
        Page<BikePath> bikePathPage = new PageImpl<>(bikePaths, pageable, totalElements);
        // Delegate to the main mapping method
        return toPagedResponse(bikePathPage);
    }

}